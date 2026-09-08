package com.eduze.manage.config;

import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.auth.service.JwtBlacklistService;
import com.eduze.manage.auth.service.JwtService;
import com.eduze.manage.common.config.AppProperties;
import com.eduze.manage.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtBlacklistService jwtBlacklistService;
    private final UserMapper userMapper;
    private final AppProperties appProperties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = resolveBearer(request);
            if (token != null) {
                authenticate(token);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void authenticate(String token) {
        try {
            Claims claims = jwtService.parse(token);
            if (!JwtService.TYPE_ACCESS.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
                return;
            }
            String jti = claims.getId();
            if (jwtBlacklistService.isBlacklisted(jti)) {
                return;
            }

            Long userId = Long.parseLong(claims.getSubject());
            Long tenantId = claims.get(JwtService.CLAIM_TENANT, Long.class);
            Long configuredTenant = appProperties.getTenant().getDefaultId();
            if (configuredTenant != null
                    && tenantId != null
                    && !configuredTenant.equals(tenantId)) {
                return;
            }

            Integer claimVersion = claims.get(JwtService.CLAIM_TOKEN_VERSION, Integer.class);
            User user = userMapper.selectById(userId);
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                return;
            }
            int currentVersion = user.getTokenVersion() == null ? 1 : user.getTokenVersion();
            if (claimVersion == null || claimVersion != currentVersion) {
                return;
            }

            TenantContext.setTenantId(tenantId);

            @SuppressWarnings("unchecked")
            List<?> rawBranchIds = claims.get(JwtService.CLAIM_BRANCHES, List.class);
            List<Long> branchIds =
                    rawBranchIds == null
                            ? List.of()
                            : rawBranchIds.stream().map(id -> ((Number) id).longValue()).toList();
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get(JwtService.CLAIM_ROLES, List.class);
            @SuppressWarnings("unchecked")
            List<String> auths = claims.get(JwtService.CLAIM_AUTHS, List.class);

            CustomUserDetails details =
                    new CustomUserDetails(
                            userId,
                            tenantId,
                            user.getUsername(),
                            user.getName(),
                            null,
                            branchIds,
                            roles != null ? new HashSet<>(roles) : new HashSet<>(),
                            auths != null ? auths : List.of(),
                            currentVersion,
                            true);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            details, null, details.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | NumberFormatException ignored) {
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveBearer(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
