package com.eduze.platform.runtime;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String supplied = request.getHeader("X-Trace-Id");
        String trace =
                supplied != null && supplied.matches("[a-zA-Z0-9-]{16,64}")
                        ? supplied
                        : UUID.randomUUID().toString();
        try {
            MDC.put("traceId", trace);
            response.setHeader("X-Trace-Id", trace);
            chain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}
