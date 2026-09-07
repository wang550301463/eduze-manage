package com.eduze.manage.student;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class StudentControllerIT extends AbstractITContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiITSupport api;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM t_course_package");
        jdbcTemplate.update("DELETE FROM t_student_guardian_relation");
        jdbcTemplate.update("DELETE FROM t_guardian");
        jdbcTemplate.update("DELETE FROM t_student WHERE enroll_no NOT LIKE 'DEMO%' OR enroll_no IS NULL");
        adminToken = api.login("admin", "admin@123");
    }

    @Test
    void crud_and_softDelete() throws Exception {
        String body =
                """
                {"branchId":1,"enrollNo":"E2026001","name":"张小明","gender":1,
                "emergencyPhone":"13800138001","mentorTeacherId":1101}
                """;
        String created = mockMvc.perform(post("/api/students")
                        .with(api.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enrollNo").value("E2026001"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper.readTree(created).get("data").get("id").asText();

        mockMvc.perform(get("/api/students/" + id).with(api.bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("张小明"));

        mockMvc.perform(put("/api/students/" + id)
                        .with(api.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"id":%s,"branchId":1,"enrollNo":"E2026001","name":"张小明2","gender":1,
                                "emergencyPhone":"13800138001","mentorTeacherId":1101}
                                """
                                        .formatted(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("张小明2"));

        mockMvc.perform(delete("/api/students/" + id).with(api.bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students/" + id).with(api.bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateEnrollNo_returnsValidationError() throws Exception {
        String body =
                """
                {"branchId":1,"enrollNo":"DUP-001","name":"学员A","gender":0,"mentorTeacherId":1101}
                """;
        mockMvc.perform(post("/api/students")
                        .with(api.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/students")
                        .with(api.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("学员A", "学员B")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void branchScope_nonSuperAdminCannotSeeOtherBranch() throws Exception {
        jdbcTemplate.update(
                """
                INSERT INTO t_branch (id, tenant_id, name, code, status, created_at, updated_at, deleted_at, version)
                VALUES (2, 1, '分校', 'B2', 1, NOW(3), NOW(3), 0, 1)
                ON DUPLICATE KEY UPDATE name = VALUES(name)
                """);
        String advisorHash = passwordEncoder.encode("advisor@123");
        jdbcTemplate.update(
                """
                INSERT INTO t_user (id, tenant_id, branch_id, username, password_hash, name, status,
                    created_at, updated_at, deleted_at, version)
                VALUES (2002, 1, 1, 'advisor1', ?, '顾问甲', 1, NOW(3), NOW(3), 0, 1)
                ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), name = VALUES(name),
                    username = VALUES(username), status = 1, deleted_at = 0
                """,
                advisorHash);
        jdbcTemplate.update("DELETE FROM t_user_role WHERE user_id = 2002");
        jdbcTemplate.update("INSERT IGNORE INTO t_user_role (id, tenant_id, user_id, role_id) VALUES (9002, 1, 2002, 3)");
        jdbcTemplate.update("DELETE FROM t_user_branch WHERE user_id = 2002");
        jdbcTemplate.update("INSERT IGNORE INTO t_user_branch (id, tenant_id, user_id, branch_id) VALUES (9002, 1, 2002, 1)");

        // 给 demo teacher 1102 在校区 2 创建副本，或者使用 SUPER_ADMIN 跨校区
        // 这里 admin 创建跨校区学员，但 mentor 必须同校区；先把 teacher_zhang 也加到 branch 2
        jdbcTemplate.update("INSERT IGNORE INTO t_user_branch (id, tenant_id, user_id, branch_id) VALUES (9102, 1, 1101, 2)");

        mockMvc.perform(post("/api/students")
                        .with(api.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"branchId":2,"enrollNo":"BR2-001","name":"分校学员","gender":0,"mentorTeacherId":1101}
                                """))
                .andExpect(status().isOk());

        String advisorToken = api.login("advisor1", "advisor@123");
        mockMvc.perform(get("/api/students").param("keyword", "BR2-001").with(api.bearer(advisorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(0)));
    }

    @Test
    void maskPhone_masksEmergencyPhone() throws Exception {
        mockMvc.perform(post("/api/students")
                        .with(api.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"branchId":1,"enrollNo":"MASK-01","name":"掩码","gender":0,
                                "emergencyPhone":"13812345678","mentorTeacherId":1101}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students").param("mask", "phone").with(api.bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].emergencyPhone").value("138****5678"));
    }

    @Test
    void updateStatus() throws Exception {
        String created = mockMvc.perform(post("/api/students")
                        .with(api.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"branchId":1,"enrollNo":"ST-01","name":"状态学员","gender":0,"mentorTeacherId":1101}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = objectMapper.readTree(created).get("data").get("id").asText();

        mockMvc.perform(patch("/api/students/" + id + "/status")
                        .with(api.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(2));
    }

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
}
