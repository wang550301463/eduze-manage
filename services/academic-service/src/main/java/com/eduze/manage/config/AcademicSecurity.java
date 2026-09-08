package com.eduze.manage.config;

import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.tenant.TenantContext;
import com.eduze.platform.runtime.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
public class AcademicSecurity {
    @Bean
    SecurityFilterChain academicChain(HttpSecurity http) throws Exception {
        return http.csrf(c -> c.disable())
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        c -> c.requestMatchers("/api/**").authenticated().anyRequest().permitAll())
                .addFilterBefore(new ActorBridge(), AnonymousAuthenticationFilter.class)
                .build();
    }

    private static final class ActorBridge extends OncePerRequestFilter {
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            try {
                if (request.getHeader("Authorization") != null) {
                    Actor actor = Actors.current();
                    TenantContext.setTenantId(Long.valueOf(actor.tenantId()));
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
                }
                chain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
