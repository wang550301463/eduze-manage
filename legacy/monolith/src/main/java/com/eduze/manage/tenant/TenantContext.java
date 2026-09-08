package com.eduze.manage.tenant;

import com.eduze.manage.common.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private static volatile Long defaultTenantId = 1L;

    private final AppProperties appProperties;

    public TenantContext(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    void initDefaultTenantId() {
        defaultTenantId = appProperties.getTenant().getDefaultId();
    }

    public static Long getTenantId() {
        Long tenantId = CURRENT.get();
        return tenantId != null ? tenantId : defaultTenantId;
    }

    /** Raw ThreadLocal value without default fallback. */
    public static Long peek() {
        return CURRENT.get();
    }

    public static void setTenantId(Long tenantId) {
        if (tenantId == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(tenantId);
        }
    }

    public static void clear() {
        CURRENT.remove();
    }
}
