package com.eduze.manage.student;

import static org.hamcrest.Matchers.hasSize;
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
class GuardianIT extends AbstractITContainerTest {

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
    }

    @Test
    void sameGuardian_canLinkMultipleStudents() throws Exception {
        Long studentA = createStudent("GA-001", "学员A");
        Long studentB = createStudent("GB-001", "学员B");

        String guardianJson = mockMvc.perform(post("/api/guardians")
                        .with(api.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"王家长","phone":"13900001111","isMainContact":1,"canPickup":1}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long guardianId = objectMapper.readTree(guardianJson).get("data").get("id").asLong();

        mockMvc.perform(post("/api/students/" + studentA + "/guardians/" + guardianId)
                        .with(api.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relation\":\"母亲\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/students/" + studentB + "/guardians/" + guardianId)
                        .with(api.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relation\":\"母亲\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students/" + studentA + "/guardians").with(api.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(get("/api/students/" + studentB + "/guardians").with(api.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].phone").value("13900001111"));
    }

    @Test
    void mainContact_switchEnforced() throws Exception {
        Long studentId = createStudent("MC-001", "主联系人学员");

        mockMvc.perform(post("/api/students/" + studentId + "/guardians/upsert")
                        .with(api.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"家长甲","phone":"13900002222","relation":"父亲","isMainContact":1}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/students/" + studentId + "/guardians/upsert")
                        .with(api.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"家长乙","phone":"13900003333","relation":"母亲","isMainContact":1}
                                """))
                .andExpect(status().isOk());

        String list = mockMvc.perform(get("/api/students/" + studentId + "/guardians").with(api.bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode guardians = objectMapper.readTree(list).get("data");
        int mainCount = 0;
        for (JsonNode g : guardians) {
            if (g.get("isMainContact").asInt() == 1) {
                mainCount++;
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(1, mainCount);
    }

    private Long createStudent(String enrollNo, String name) throws Exception {
        String json = mockMvc.perform(post("/api/students")
                        .with(api.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"branchId":1,"enrollNo":"%s","name":"%s","gender":0,"mentorTeacherId":1101}
                                """
                                        .formatted(enrollNo, name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json).get("data").get("id").asLong();
    }
}
