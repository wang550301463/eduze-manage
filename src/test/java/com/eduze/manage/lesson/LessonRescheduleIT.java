package com.eduze.manage.lesson;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

class LessonRescheduleIT extends AbstractApiIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;
    private long lessonId;

    @BeforeEach
    void seed() throws Exception {
        token = adminToken();
        String courseName = "调课课-" + UUID.randomUUID();
        long courseId = objectMapper
                .readTree(mockMvc.perform(post("/api/courses")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"" + courseName + "\",\"lessonMinutes\":60}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();

        long groupId = objectMapper
                .readTree(mockMvc.perform(post("/api/class-groups")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"branchId":1,"name":"调课班","courseId":%d,"capacity":10}
                                        """
                                                .formatted(courseId)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();

        lessonId = objectMapper
                .readTree(mockMvc.perform(post("/api/lessons")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "branchId": 1,
                                          "classGroupId": %d,
                                          "startAt": "2026-05-20T09:00:00",
                                          "endAt": "2026-05-20T10:30:00"
                                        }
                                        """
                                                .formatted(groupId)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();
    }

    @Test
    void reschedule_success() throws Exception {
        mockMvc.perform(post("/api/lessons/" + lessonId + "/reschedule")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "startAt": "2026-05-21T09:00:00",
                                  "endAt": "2026-05-21T10:30:00",
                                  "reason": "教师请假"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void reschedule_completedLesson_returns422() throws Exception {
        jdbcTemplate.update("UPDATE t_lesson SET status='COMPLETED' WHERE id=?", lessonId);
        mockMvc.perform(post("/api/lessons/" + lessonId + "/reschedule")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "startAt": "2026-05-22T09:00:00",
                                  "endAt": "2026-05-22T10:30:00",
                                  "reason": "调整"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void reschedule_withTeacherConflict_returns409() throws Exception {
        long courseId = objectMapper
                .readTree(mockMvc.perform(post("/api/courses")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"调课冲突课-" + UUID.randomUUID() + "\",\"lessonMinutes\":60}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();
        long otherGroupId = objectMapper
                .readTree(mockMvc.perform(post("/api/class-groups")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"branchId":1,"name":"调课冲突班","courseId":%d,"capacity":10}
                                        """
                                                .formatted(courseId)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();

        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "branchId": 1,
                                  "classGroupId": %d,
                                  "teacherId": 1101,
                                  "startAt": "2026-05-25T09:00:00",
                                  "endAt": "2026-05-25T10:30:00"
                                }
                                """
                                        .formatted(otherGroupId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lessons/" + lessonId + "/reschedule")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "startAt": "2026-05-25T09:30:00",
                                  "endAt": "2026-05-25T11:00:00",
                                  "teacherId": 1101,
                                  "reason": "撞期"
                                }
                                """))
                .andExpect(status().isConflict());
    }
}
