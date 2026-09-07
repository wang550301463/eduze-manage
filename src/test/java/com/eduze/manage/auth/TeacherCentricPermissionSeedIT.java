package com.eduze.manage.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduze.manage.AbstractITContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class TeacherCentricPermissionSeedIT extends AbstractITContainerTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void sixNewPermissionsExist() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_permission WHERE code IN "
                        + "('teacher:availability:read','teacher:availability:write',"
                        + "'subscription:read','subscription:write',"
                        + "'student:mentor_assign','lesson:teacher_view')",
                Integer.class);
        assertThat(count).isEqualTo(6);
    }

    @Test
    void teacherRoleHasAvailabilityWrite() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_role_permission rp "
                        + "JOIN t_permission p ON p.id=rp.permission_id "
                        + "WHERE rp.role_id=4 AND p.code='teacher:availability:write'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void advisorHasSubscriptionWrite() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_role_permission rp "
                        + "JOIN t_permission p ON p.id=rp.permission_id "
                        + "WHERE rp.role_id=3 AND p.code='subscription:write'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void superAdminHasAllNewPermissions() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_role_permission rp "
                        + "JOIN t_permission p ON p.id=rp.permission_id "
                        + "WHERE rp.role_id=1 AND p.code IN ("
                        + "'teacher:availability:read','teacher:availability:write',"
                        + "'subscription:read','subscription:write',"
                        + "'student:mentor_assign','lesson:teacher_view')",
                Integer.class);
        assertThat(count).isEqualTo(6);
    }
}
