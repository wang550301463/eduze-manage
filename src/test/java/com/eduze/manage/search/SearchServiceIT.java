package com.eduze.manage.search;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.support.ApiITSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SearchServiceIT extends AbstractITContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiITSupport api;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM t_student_guardian_relation");
        jdbcTemplate.update("DELETE FROM t_guardian");
        jdbcTemplate.update("DELETE FROM t_student WHERE enroll_no NOT LIKE 'DEMO%' OR enroll_no IS NULL");
        token = api.login("admin", "admin@123");

        mockMvc.perform(post("/api/students")
                        .with(api.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"branchId":1,"enrollNo":"ZHANG-001","name":"张测试","gender":0,"mentorTeacherId":1101}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/guardians")
                        .with(api.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"张家长","phone":"13911112222","isMainContact":0,"canPickup":1}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void searchByName_returnsStudentsAndGuardians() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "张").param("types", "student,guardian").with(api.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void exactEnrollNo_ranksFirstAmongStudents() throws Exception {
        String body = mockMvc.perform(get("/api/search")
                        .param("q", "ZHANG-001")
                        .param("types", "student")
                        .with(api.bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode hits = objectMapper.readTree(body).get("data");
        assertEquals("student", hits.get(0).get("type").asText());
        assertEquals(true, hits.get(0).get("subtitle").asText().startsWith("ZHANG-001"));
    }
}
