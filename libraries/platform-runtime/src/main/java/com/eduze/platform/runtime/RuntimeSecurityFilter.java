package com.eduze.platform.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(
        name = "eduze.runtime.security.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class RuntimeSecurityFilter extends OncePerRequestFilter {
    private final InternalCredentials credentials;
    private final InternalClient client;
    private final ObjectMapper json;

    @org.springframework.beans.factory.annotation.Value(
            "${eduze.runtime.security.signed-upload-enabled:false}")
    private boolean signedUploadEnabled;

    @org.springframework.beans.factory.annotation.Value(
            "${eduze.runtime.security.payment-callbacks-enabled:false}")
    private boolean paymentCallbacksEnabled;

    @org.springframework.beans.factory.annotation.Value(
            "${eduze.runtime.security.public-enquiries-enabled:false}")
    private boolean publicEnquiriesEnabled;

    public RuntimeSecurityFilter(
            InternalCredentials credentials, InternalClient client, ObjectMapper json) {
        this.credentials = credentials;
        this.client = client;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String path = request.getRequestURI();
            boolean internal = path.startsWith("/internal/");
            boolean publicRead =
                    ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))
                            && path.matches("/api/v1/[^/]+/public(?:/.*)?");
            boolean signedUpload =
                    signedUploadEnabled
                            && "PUT".equals(request.getMethod())
                            && path.matches("/api/v1/media/public/uploads/[a-f0-9-]{36}");
            boolean paymentCallback =
                    paymentCallbacksEnabled
                            && "POST".equals(request.getMethod())
                            && (path.equals("/api/v1/commerce/callbacks/payment")
                                    || path.equals("/api/v1/commerce/callbacks/refund"));
            boolean publicEnquiry =
                    publicEnquiriesEnabled
                            && "POST".equals(request.getMethod())
                            && path.matches(
                                    "/api/v1/engagement/public/studios/[a-zA-Z0-9-]+/enquiries");
            if (internal) {
                credentials.require(request);
            }
            String bearer = request.getHeader("Authorization");
            if (bearer != null && bearer.startsWith("Bearer ")) {
                Actor actor =
                        client.post(
                                "identity",
                                "/internal/identity/introspect",
                                Map.of("token", bearer.substring(7)),
                                Actor.class);
                if (actor == null) {
                    throw new PlatformException(401, "请重新登录");
                }
                request.setAttribute(Actors.ATTRIBUTE, actor);
            } else if (!internal
                    && !publicRead
                    && !signedUpload
                    && !paymentCallback
                    && !publicEnquiry
                    && !path.startsWith("/actuator/")
                    && !path.equals("/error")) {
                throw new PlatformException(401, "请先登录");
            }
            chain.doFilter(request, response);
        } catch (PlatformException exception) {
            response.setStatus(exception.getStatus());
            response.setContentType("application/json;charset=UTF-8");
            json.writeValue(
                    response.getOutputStream(),
                    ApiResponse.error(exception.getStatus(), exception.getMessage()));
        } finally {
            request.removeAttribute(Actors.ATTRIBUTE);
        }
    }
}
