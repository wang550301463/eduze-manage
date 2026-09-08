package com.eduze.manage.config;

import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.identity.SessionIntrospection;
import com.eduze.manage.tenant.TenantContext;
import com.eduze.platform.runtime.Actor;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final SessionIntrospection sessions;
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    private final com.eduze.platform.runtime.InternalCredentials internalCredentials;

    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            if (request.getRequestURI().startsWith("/internal/")) {
                internalCredentials.require(request);
            }
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    Actor actor = sessions.introspect(header.substring(7).trim());
                    request.setAttribute(com.eduze.platform.runtime.Actors.ATTRIBUTE, actor);
                    CustomUserDetails u =
                            new CustomUserDetails(
                                    Long.valueOf(actor.userId()),
                                    Long.valueOf(actor.tenantId()),
                                    actor.userId(),
                                    actor.userId(),
                                    null,
                                    actor.branchIds().stream().map(Long::valueOf).toList(),
                                    actor.roles(),
                                    actor.permissions(),
                                    1,
                                    true);
                    SecurityContextHolder.getContext()
                            .setAuthentication(
                                    new UsernamePasswordAuthenticationToken(
                                            u, null, u.getAuthorities()));
                } catch (com.eduze.platform.runtime.PlatformException ex) {
                    SecurityContextHolder.clearContext();
                }
            }
            chain.doFilter(request, response);
        } catch (com.eduze.platform.runtime.PlatformException ex) {
            response.setStatus(ex.getStatus());
            response.setContentType("application/json;charset=UTF-8");
            json.writeValue(
                    response.getOutputStream(),
                    com.eduze.platform.runtime.ApiResponse.error(ex.getStatus(), ex.getMessage()));
        } finally {
            TenantContext.clear();
            request.removeAttribute(com.eduze.platform.runtime.Actors.ATTRIBUTE);
        }
    }
}
