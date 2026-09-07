package com.eduze.manage.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.common.util.IdGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class SearchServiceFullIT extends AbstractITContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String principalToken;

    private long branch1Id = 1L;
    private long branch2Id;
    private long saturdayClassId;
    private long saturdayLessonId;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM t_lesson WHERE tenant_id = 1");
        jdbcTemplate.update("DELETE FROM t_student_class_group WHERE tenant_id = 1");
        jdbcTemplate.update("DELETE FROM t_class_group WHERE tenant_id = 1");
        jdbcTemplate.update("DELETE FROM t_course WHERE tenant_id = 1");
        jdbcTemplate.update("DELETE FROM t_student_guardian_relation WHERE tenant_id = 1");
        jdbcTemplate.update("DELETE FROM t_guardian WHERE tenant_id = 1");
        jdbcTemplate.update(
                "DELETE FROM t_student WHERE tenant_id = 1 AND (enroll_no NOT LIKE 'DEMO%' OR enroll_no IS NULL)");
        // 保留内置 admin(1001) 与 demo 老师(1101–1103)
        jdbcTemplate.update("DELETE FROM t_user_branch WHERE user_id > 1103");
        jdbcTemplate.update("DELETE FROM t_user_role WHERE user_id > 1103");
        jdbcTemplate.update("DELETE FROM t_user WHERE id > 1103");
        jdbcTemplate.update("DELETE FROM t_branch WHERE id > 1");

        branch2Id = IdGenerator.nextId();
        jdbcTemplate.update(
                "INSERT INTO t_branch (id, tenant_id, name, code, status) VALUES (?, 1, '西城校区', 'WEST', 1)",
                branch2Id);

        long courseId = IdGenerator.nextId();
        jdbcTemplate.update(
                "INSERT INTO t_course (id, tenant_id, name, lesson_minutes) VALUES (?, 1, '启蒙·4-6岁', 60)",
                courseId);

        saturdayClassId = IdGenerator.nextId();
        jdbcTemplate.update(
                """
                INSERT INTO t_class_group (id, tenant_id, branch_id, name, course_id, capacity, status)
                VALUES (?, 1, ?, '周六上午启蒙A班', ?, 20, 1)
                """,
                saturdayClassId,
                branch1Id,
                courseId);

        long branch2ClassId = IdGenerator.nextId();
        jdbcTemplate.update(
                """
                INSERT INTO t_class_group (id, tenant_id, branch_id, name, course_id, capacity, status)
                VALUES (?, 1, ?, '周六下午启蒙B班', ?, 20, 1)
                """,
                branch2ClassId,
                branch2Id,
                courseId);

        long student1 = IdGenerator.nextId();
        jdbcTemplate.update(
                """
                INSERT INTO t_student (id, tenant_id, branch_id, enroll_no, name, gender, status)
                VALUES (?, 1, ?, 'E001', '张周六', 1, 1)
                """,
                student1,
                branch1Id);

        long student2 = IdGenerator.nextId();
        jdbcTemplate.update(
                """
                INSERT INTO t_student (id, tenant_id, branch_id, enroll_no, name, gender, status)
                VALUES (?, 1, ?, 'E002', '李西城', 2, 1)
                """,
                student2,
                branch2Id);

        long guardianId = IdGenerator.nextId();
        jdbcTemplate.update(
                "INSERT INTO t_guardian (id, tenant_id, name, phone) VALUES (?, 1, '周六家长', '13800001111')",
                guardianId);
        jdbcTemplate.update(
                """
                INSERT INTO t_student_guardian_relation (id, tenant_id, student_id, guardian_id, relation)
                VALUES (?, 1, ?, ?, '妈妈')
                """,
                IdGenerator.nextId(),
                student1,
                guardianId);

        saturdayLessonId = IdGenerator.nextId();
        jdbcTemplate.update(
                """
                INSERT INTO t_lesson (id, tenant_id, branch_id, class_group_id, teacher_id, start_at, end_at, status)
                VALUES (?, 1, ?, ?, 1001, ?, ?, 1)
                """,
                saturdayLessonId,
                branch1Id,
                saturdayClassId,
                LocalDateTime.of(2026, 5, 11, 9, 0),
                LocalDateTime.of(2026, 5, 11, 10, 0));

        jdbcTemplate.update(
                """
                INSERT INTO t_lesson (id, tenant_id, branch_id, class_group_id, teacher_id, start_at, end_at, status)
                VALUES (?, 1, ?, ?, 1001, ?, ?, 1)
                """,
                IdGenerator.nextId(),
                branch2Id,
                branch2ClassId,
                LocalDateTime.of(2026, 5, 11, 14, 0),
                LocalDateTime.of(2026, 5, 11, 15, 0));

        adminToken = login("admin", "admin@123");

        long principalId = IdGenerator.nextId();
        jdbcTemplate.update(
                """
                INSERT INTO t_user (id, tenant_id, branch_id, username, password_hash, name, status)
                VALUES (?, 1, ?, 'principal1',
                '$2a$10$Vc9KSaM9T4nAJVhrG3kacu.930c1dN/FnftTYp7twKW2oaq.PF0iW', '西城校长', 1)
                """,
                principalId,
                branch2Id);
        jdbcTemplate.update(
                "INSERT INTO t_user_role (id, tenant_id, user_id, role_id) VALUES (?, 1, ?, 2)",
                IdGenerator.nextId(),
                principalId);
        jdbcTemplate.update(
                "INSERT INTO t_user_branch (id, tenant_id, user_id, branch_id) VALUES (?, 1, ?, ?)",
                IdGenerator.nextId(),
                principalId,
                branch2Id);

        principalToken = login("principal1", "admin@123");
    }

    @Test
    void searchSaturday_returnsClassGroupAndLesson() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "周六").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.type=='class_group')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.type=='lesson')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.type=='student')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.type=='guardian')]").isNotEmpty());
    }

    @Test
    void searchSaturday_principalOnlySeesOwnBranch() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/api/search").param("q", "周六").header("Authorization", "Bearer " + principalToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.isArray()).isTrue();
        for (JsonNode hit : data) {
            if (hit.hasNonNull("branchId")) {
                assertThat(hit.get("branchId").asLong()).isEqualTo(branch2Id);
            }
        }
        assertThat(data.findValues("title").stream()
                        .anyMatch(n -> n.asText().contains("周六上午")))
                .isFalse();
    }

    @Test
    void searchExactEnrollNo_rankedFirst() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/api/search")
                                .param("q", "E001")
                                .param("types", "student")
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("data");
        assertThat(data.size()).isGreaterThan(0);
        assertThat(data.get(0).get("subtitle").asText()).startsWith("E001");
        assertThat(data.get(0).get("type").asText()).isEqualTo("student");
    }

    private String login(String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper
                .readTree(loginResult.getResponse().getContentAsString())
                .get("data")
                .get("accessToken")
                .asText();
    }
}
