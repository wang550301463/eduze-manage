package com.eduze.manage.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.tenant.demo.ScopeStudent;
import com.eduze.manage.tenant.demo.mapper.ScopeStudentMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class BranchScopeIT extends AbstractITContainerTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private ScopeStudentMapper scopeStudentMapper;

    @BeforeEach
    void setUpStudents() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS t_student (
                    id BIGINT NOT NULL PRIMARY KEY,
                    tenant_id BIGINT NOT NULL DEFAULT 1,
                    branch_id BIGINT NOT NULL,
                    name VARCHAR(64) NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    deleted_at BIGINT NOT NULL DEFAULT 0,
                    created_by BIGINT NULL,
                    updated_by BIGINT NULL,
                    version INT NOT NULL DEFAULT 1
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.update("DELETE FROM t_student WHERE id IN (1, 2)");
        jdbcTemplate.update(
                """
                INSERT INTO t_student (id, tenant_id, branch_id, enroll_no, name, status) VALUES
                (1, 1, 1, 'SCOPE-1', '校区1学员', 1),
                (2, 1, 2, 'SCOPE-2', '校区2学员', 1)
                ON DUPLICATE KEY UPDATE name = VALUES(name), branch_id = VALUES(branch_id)
                """);
        TenantContext.setTenantId(1L);
    }

    @Test
    void nonSuperAdmin_onlySeesOwnBranchStudents() {
        CustomUserDetails details =
                new CustomUserDetails(
                        200L,
                        1L,
                        "scoped",
                        "scoped",
                        "",
                        List.of(1L),
                        java.util.Set.of("ADVISOR"),
                        List.of("student:read"),
                        1,
                        true);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                details, null, details.getAuthorities()));

        List<ScopeStudent> rows = scopeStudentMapper.selectList(null);
        assertTrue(rows.stream().anyMatch(r -> "校区1学员".equals(r.getName())));
        assertTrue(rows.stream().noneMatch(r -> "校区2学员".equals(r.getName())));
        assertEquals(1L, rows.stream().filter(r -> r.getId() == 1L).count());
    }
}
