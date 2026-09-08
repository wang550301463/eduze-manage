package com.eduze.manage.branch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.AbstractITContainerTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class BranchControllerIT extends AbstractITContainerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAdvisor() {
        String hash = passwordEncoder.encode("advisor@123");
        jdbcTemplate.update("DELETE FROM t_user_branch WHERE user_id = 300");
        jdbcTemplate.update("DELETE FROM t_user_role WHERE user_id = 300");
        jdbcTemplate.update("DELETE FROM t_user WHERE id = 300 OR username = 'advisor_branch_it'");
        jdbcTemplate.update(
                """
                INSERT INTO t_user (id, tenant_id, username, password_hash, name, status, token_version)
                VALUES (300, 1, 'advisor_branch_it', ?, '顾问', 1, 1)
                """,
                hash);
        jdbcTemplate.update("INSERT INTO t_user_role (id, user_id, role_id) VALUES (3001, 300, 3)");
        jdbcTemplate.update(
                "INSERT INTO t_user_branch (id, user_id, branch_id) VALUES (3001, 300, 1)");
    }

    @Test
    void superAdmin_crudBranch_advisorForbidden() throws Exception {
        String adminToken = loginToken("admin", "admin@123");

        MvcResult createResult =
                mockMvc.perform(
                                post("/api/branches")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                {"name":"测试校区","code":"TEST01","address":"测试地址"}
                                """))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.code").value("TEST01"))
                        .andReturn();

        long branchId =
                objectMapper
                        .readTree(createResult.getResponse().getContentAsString())
                        .get("data")
                        .get("id")
                        .asLong();

        mockMvc.perform(
                        put("/api/branches/" + branchId)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name":"测试校区改","code":"TEST01","address":"新地址"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试校区改"));

        String advisorToken = loginToken("advisor_branch_it", "advisor@123");
        mockMvc.perform(get("/api/branches").header("Authorization", "Bearer " + advisorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        delete("/api/branches/" + branchId)
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void deleteBranchWithStudents_returnsConflict() throws Exception {
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
        jdbcTemplate.update(
                """
                INSERT INTO t_branch (id, tenant_id, name, code, status)
                VALUES (99, 1, '有学员校区', 'HAS_STU', 1)
                ON DUPLICATE KEY UPDATE name = VALUES(name)
                """);
        jdbcTemplate.update("DELETE FROM t_student WHERE branch_id = 99");
        jdbcTemplate.update(
                "INSERT INTO t_student (id, tenant_id, branch_id, enroll_no, name, status) VALUES (99, 1, 99, 'E99', '学员', 1)");

        String adminToken = loginToken("admin", "admin@123");
        mockMvc.perform(delete("/api/branches/99").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("校区下仍有学员，无法删除"));
    }

    private String loginToken(String username, String password) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"username\":\""
                                                        + username
                                                        + "\",\"password\":\""
                                                        + password
                                                        + "\"}"))
                        .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("data").get("accessToken").asText();
    }
}
