package com.eduze.platform.runtime;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;

@Component
public class InternalCredentials {
    private final RuntimeProperties properties;

    public InternalCredentials(RuntimeProperties properties) {
        this.properties = properties;
    }

    public String require(HttpServletRequest request) {
        String caller = request.getHeader("X-Service-Name");
        String supplied = request.getHeader("X-Service-Token");
        String expected = caller == null ? null : properties.getCallers().get(caller);
        if (expected == null
                || expected.length() < 24
                || supplied == null
                || !MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new PlatformException(401, "内部调用身份无效");
        }
        request.setAttribute("eduze.serviceCaller", caller);
        return caller;
    }

    public void requireCaller(HttpServletRequest request, String caller) {
        if (!caller.equals(require(request))) {
            throw new PlatformException(403, "内部调用无权执行此操作");
        }
    }
}
