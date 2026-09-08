package com.eduze.manage.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class RbacIT extends AbstractITContainerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedTeacher() {
        String hash = passwordEncoder.encode("teacher@123");
        jdbcTemplate.update("DELETE FROM t_user_role WHERE user_id = 200");
        jdbcTemplate.update("DELETE FROM t_user_branch WHERE user_id = 200");
        jdbcTemplate.update("DELETE FROM t_user WHERE id = 200");
        jdbcTemplate.update(
                """
                INSERT INTO t_user (id, tenant_id, username, password_hash, name, status)
                VALUES (200, 1, 'teacher1', ?, '测试教师', 1)
                """,
                hash);
        jdbcTemplate.update("INSERT INTO t_user_role (id, user_id, role_id) VALUES (2001, 200, 4)");
        jdbcTemplate.update(
                "INSERT INTO t_user_branch (id, user_id, branch_id) VALUES (2001, 200, 1)");
    }

    @Test
    void teacherCannotListUsers_superAdminCan() throws Exception {
        String teacherToken = loginToken("teacher1", "teacher@123");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());

        String adminToken = loginToken("admin", "admin@123");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
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
