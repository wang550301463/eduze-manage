package com.eduze.manage.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.eduze.manage.AbstractITContainerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TenantContextIT extends AbstractITContainerTest {

    @Autowired
    private TenantContext tenantContext;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getTenantId_returnsDefaultWhenNotSet() {
        TenantContext.clear();
        assertEquals(1L, TenantContext.getTenantId());
    }

    @Test
    void getTenantId_returnsSetValue() {
        TenantContext.setTenantId(99L);
        assertEquals(99L, TenantContext.getTenantId());
    }

    @Test
    void clear_resetsToDefault() {
        TenantContext.setTenantId(99L);
        TenantContext.clear();
        assertEquals(1L, TenantContext.getTenantId());
    }
}
