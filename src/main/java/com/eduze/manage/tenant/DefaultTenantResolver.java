package com.eduze.manage.tenant;

import com.eduze.manage.common.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class DefaultTenantResolver implements TenantResolver {

    private final AppProperties appProperties;

    public DefaultTenantResolver(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public Long resolve(HttpServletRequest request) {
        return appProperties.getTenant().getDefaultId();
    }
}
