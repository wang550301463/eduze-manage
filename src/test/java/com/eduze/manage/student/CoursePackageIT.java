package com.eduze.manage.student;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.support.ApiITSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CoursePackageIT extends AbstractITContainerTest {

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
        jdbcTemplate.update("DELETE FROM t_course_package");
        jdbcTemplate.update("DELETE FROM t_student WHERE enroll_no NOT LIKE 'DEMO%' OR enroll_no IS NULL");
        token = api.login("admin", "admin@123");
    }

    @Test
    void twoPackages_aggregateBalance_excludesExpired() throws Exception {
        String created = mockMvc.perform(post("/api/students")
                        .with(api.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"branchId":1,"enrollNo":"PKG-01","name":"课时学员","gender":0,"mentorTeacherId":1101}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long studentId = objectMapper.readTree(created).get("data").get("id").asLong();

        String future = LocalDate.now().plusMonths(1).toString();
        String past = LocalDate.now().minusMonths(1).toString();

        mockMvc.perform(post("/api/students/" + studentId + "/packages")
                        .with(api.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"totalLessons":20,"remainingLessons":3,"expireDate":"%s","note":"有效包"}
                                """
                                        .formatted(future)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/students/" + studentId + "/packages")
                        .with(api.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"totalLessons":10,"remainingLessons":100,"expireDate":"%s","note":"过期包"}
                                """
                                        .formatted(past)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students/" + studentId).with(api.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRemaining").value(3))
                .andExpect(jsonPath("$.data.alertLow").value(true));
    }
}
