package com.eduze.manage.tenant;

import jakarta.servlet.http.HttpServletRequest;

public interface TenantResolver {

    Long resolve(HttpServletRequest request);
}
