package com.eduze.manage.integration;

import com.eduze.platform.runtime.PlatformException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class IntegrationScope {
    private final Environment env;

    public IntegrationScope(Environment env) {
        this.env = env;
    }

    public String tenant() {
        return env.getRequiredProperty("eduze.tenant.default-id");
    }

    public void requireTenant(String tenantId) {
        if (!tenant().equals(tenantId)) throw new PlatformException(403, "机构范围不匹配");
    }

    public void requireId(String id) {
        if (id == null || !id.matches("[A-Za-z0-9-]{1,64}"))
            throw new PlatformException(400, "业务 ID 无效");
    }
}
