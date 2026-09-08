package com.eduze.manage.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.dto.LoginRequest;
import com.eduze.manage.auth.dto.LoginResponse;
import com.eduze.manage.auth.dto.RefreshRequest;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.auth.security.CustomUserDetailsService;
import com.eduze.manage.branch.domain.Branch;
import com.eduze.manage.branch.mapper.BranchMapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final BranchMapper branchMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final LoginAttemptService loginAttemptService;
    private final JwtBlacklistService jwtBlacklistService;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenVersionService tokenVersionService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        loginAttemptService.checkLocked(request.getUsername());

        User user =
                userMapper.selectOne(
                        Wrappers.<User>lambdaQuery()
                                .eq(User::getTenantId, TenantContext.getTenantId())
                                .eq(User::getUsername, request.getUsername()));
        if (user == null
                || user.getStatus() == null
                || user.getStatus() != 1
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptService.incrementFail(request.getUsername());
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }

        loginAttemptService.reset(request.getUsername());
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        CustomUserDetails details = userDetailsService.loadByUserId(user.getId());
        return issueTokens(details);
    }

    public LoginResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parse(request.getRefreshToken());
        } catch (JwtException ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "无效的刷新令牌");
        }
        if (!JwtService.TYPE_REFRESH.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "无效的刷新令牌");
        }
        String jti = claims.getId();
        if (!refreshTokenStore.isActive(jti) || jwtBlacklistService.isBlacklisted(jti)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "令牌已失效");
        }

        Long userId = Long.parseLong(claims.getSubject());
        Integer claimVersion = claims.get(JwtService.CLAIM_TOKEN_VERSION, Integer.class);
        int currentVersion = tokenVersionService.current(userId);
        if (claimVersion == null || claimVersion != currentVersion) {
            refreshTokenStore.revoke(jti);
            throw new BizException(ErrorCode.UNAUTHORIZED, "令牌已失效");
        }

        CustomUserDetails details = userDetailsService.loadByUserId(userId);
        if (!details.isEnabled()) {
            refreshTokenStore.revoke(jti);
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号已禁用");
        }

        refreshTokenStore.revoke(jti);
        jwtBlacklistService.blacklist(jti, jwtService.getExpiration(claims));
        return issueTokens(details, claims.get(JwtService.CLAIM_ACTIVE_ROLE, String.class));
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                Claims claims = jwtService.parse(accessToken);
                jwtBlacklistService.blacklist(claims.getId(), jwtService.getExpiration(claims));
            } catch (JwtException ignored) {
                // ignore invalid token on logout
            }
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                Claims claims = jwtService.parse(refreshToken);
                if (JwtService.TYPE_REFRESH.equals(
                        claims.get(JwtService.CLAIM_TYPE, String.class))) {
                    refreshTokenStore.revoke(claims.getId());
                    jwtBlacklistService.blacklist(claims.getId(), jwtService.getExpiration(claims));
                }
            } catch (JwtException ignored) {
                // ignore invalid refresh on logout
            }
        }
    }

    public LoginResponse issueTokens(CustomUserDetails details) {
        return issueTokens(details, null);
    }

    public LoginResponse issueTokens(CustomUserDetails original, String activeRole) {
        CustomUserDetails details =
                "PARENT".equals(activeRole)
                        ? new CustomUserDetails(
                                original.getUserId(),
                                original.getTenantId(),
                                original.getUsername(),
                                original.getDisplayName(),
                                null,
                                List.of(),
                                java.util.Set.of("PARENT"),
                                List.of(),
                                original.getTokenVersion(),
                                original.isEnabled())
                        : original;
        String accessJti = jwtService.newJti();
        String refreshJti = jwtService.newJti();
        int tokenVersion = details.getTokenVersion() <= 0 ? 1 : details.getTokenVersion();
        List<Long> branchIds = details.getBranchIds();
        List<String> auths = details.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        List<String> roles = List.copyOf(details.getRoleCodes());

        String accessToken =
                jwtService.generateScoped(
                        details.getUserId(),
                        accessJti,
                        details.getTenantId(),
                        tokenVersion,
                        branchIds,
                        roles,
                        auths,
                        JwtService.TYPE_ACCESS,
                        activeRole);
        String refreshToken =
                jwtService.generateScoped(
                        details.getUserId(),
                        refreshJti,
                        details.getTenantId(),
                        tokenVersion,
                        branchIds,
                        roles,
                        auths,
                        JwtService.TYPE_REFRESH,
                        activeRole);
        refreshTokenStore.store(
                refreshJti,
                details.getUserId(),
                Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()));

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(buildUserInfo(details))
                .build();
    }

    private LoginResponse.UserInfo buildUserInfo(CustomUserDetails details) {
        List<LoginResponse.BranchInfo> branches = new ArrayList<>();
        if (!details.getBranchIds().isEmpty()) {
            List<Branch> branchList = branchMapper.selectBatchIds(details.getBranchIds());
            for (Branch branch : branchList) {
                branches.add(
                        LoginResponse.BranchInfo.builder()
                                .id(branch.getId())
                                .name(branch.getName())
                                .code(branch.getCode())
                                .build());
            }
        }
        return LoginResponse.UserInfo.builder()
                .id(details.getUserId())
                .name(details.getDisplayName())
                .username(details.getUsername())
                .roles(List.copyOf(details.getRoleCodes()))
                .permissions(details.getAuthorities().stream().map(a -> a.getAuthority()).toList())
                .branches(branches)
                .build();
    }
}
