package com.eduze.manage.tenant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.support.ApiITSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class BranchAccessGuardIT extends AbstractITContainerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ApiITSupport api;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private PasswordEncoder passwordEncoder;

    private String scopedToken;

    @BeforeEach
    void setUp() throws Exception {
        String hash = passwordEncoder.encode("advisor@123");
        jdbcTemplate.update(
                """
                INSERT INTO t_user (id, tenant_id, branch_id, username, password_hash, name, status,
                    token_version, created_at, updated_at, deleted_at, version)
                VALUES (2002, 1, 1, 'advisor_guard_it', ?, '顾问甲', 1, 1, NOW(3), NOW(3), 0, 1)
                ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), name = VALUES(name),
                    status = 1, token_version = 1, deleted_at = 0, username = 'advisor_guard_it'
                """,
                hash);
        jdbcTemplate.update("DELETE FROM t_user_role WHERE user_id = 2002");
        jdbcTemplate.update(
                "INSERT IGNORE INTO t_user_role (id, tenant_id, user_id, role_id) VALUES (9002, 1, 2002, 3)");
        jdbcTemplate.update("DELETE FROM t_user_branch WHERE user_id = 2002");
        jdbcTemplate.update(
                "INSERT IGNORE INTO t_user_branch (id, tenant_id, user_id, branch_id) VALUES (9002, 1, 2002, 1)");
        jdbcTemplate.update(
                """
                INSERT INTO t_branch (id, tenant_id, name, code, status, created_at, updated_at, deleted_at, version)
                VALUES (2, 1, '分校', 'B2', 1, NOW(3), NOW(3), 0, 1)
                ON DUPLICATE KEY UPDATE name = VALUES(name)
                """);
        scopedToken = api.login("advisor_guard_it", "advisor@123");
    }

    @Test
    void listStudents_otherBranch_forbidden() throws Exception {
        mockMvc.perform(get("/api/students").param("branchId", "2").with(api.bearer(scopedToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void attendanceToday_otherBranch_forbidden() throws Exception {
        mockMvc.perform(
                        get("/api/attendance/today")
                                .param("branchId", "2")
                                .with(api.bearer(scopedToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createStudent_otherBranch_forbidden() throws Exception {
        mockMvc.perform(
                        post("/api/students")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"branchId":2,"enrollNo":"XBR-001","name":"跨校区","gender":0,"mentorTeacherId":1101}
                                """)
                                .with(api.bearer(scopedToken)))
                .andExpect(status().isForbidden());
    }
}
