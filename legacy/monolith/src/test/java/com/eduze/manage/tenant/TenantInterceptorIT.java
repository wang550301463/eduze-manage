package com.eduze.manage.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.tenant.demo.TenantDemo;
import com.eduze.manage.tenant.demo.mapper.TenantDemoMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class TenantInterceptorIT extends AbstractITContainerTest {

    @Autowired private TenantDemoMapper tenantDemoMapper;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpDemoTable() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS t_tenant_demo (
                    id BIGINT NOT NULL PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    branch_id BIGINT NULL,
                    name VARCHAR(64) NULL,
                    created_at DATETIME(3) NOT NULL,
                    updated_at DATETIME(3) NOT NULL,
                    deleted_at BIGINT NOT NULL DEFAULT 0,
                    created_by BIGINT NULL,
                    updated_by BIGINT NULL,
                    version INT NOT NULL DEFAULT 1
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.update("DELETE FROM t_tenant_demo");
        jdbcTemplate.update(
                """
                INSERT INTO t_tenant_demo
                    (id, tenant_id, name, created_at, updated_at, deleted_at, version)
                VALUES
                    (1, 1, 'tenant-1', NOW(3), NOW(3), 0, 1),
                    (2, 2, 'tenant-2', NOW(3), NOW(3), 0, 1)
                """);
    }

    @Test
    void selectList_filtersByTenantContext() {
        TenantContext.setTenantId(1L);
        List<TenantDemo> tenantOneRows = tenantDemoMapper.selectList(null);
        assertEquals(1, tenantOneRows.size());
        assertEquals("tenant-1", tenantOneRows.get(0).getName());

        TenantContext.setTenantId(2L);
        List<TenantDemo> tenantTwoRows = tenantDemoMapper.selectList(null);
        assertEquals(1, tenantTwoRows.size());
        assertEquals("tenant-2", tenantTwoRows.get(0).getName());
    }

    @Test
    void insert_autoFillsTenantId() {
        TenantContext.setTenantId(1L);
        TenantDemo demo = new TenantDemo();
        demo.setName("auto-tenant");
        tenantDemoMapper.insert(demo);

        TenantDemo inserted = tenantDemoMapper.selectById(demo.getId());
        assertEquals(1L, inserted.getTenantId());
        assertTrue(inserted.getCreatedAt() != null);
    }
}
