package com.eduze.platform.runtime;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Request-local identity. No mutable static user state. */
public final class Actors {
    public static final String ATTRIBUTE = Actor.class.getName();

    private Actors() {}

    public static Actor current() {
        Actor actor = optional();
        if (actor == null) {
            throw new PlatformException(401, "请先登录");
        }
        return actor;
    }

    public static Actor optional() {
        HttpServletRequest request = request();
        return request == null ? null : (Actor) request.getAttribute(ATTRIBUTE);
    }

    public static HttpServletRequest request() {
        return RequestContextHolder.getRequestAttributes()
                        instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }
}
