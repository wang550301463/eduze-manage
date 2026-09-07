package com.eduze.manage.lesson;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class LessonConflictWriteIT extends AbstractApiIT {

    private String token;
    private long classGroupId;
    private long otherGroupId;

    @BeforeEach
    void seed() throws Exception {
        token = adminToken();
        classGroupId = createGroup("冲突写班");
        otherGroupId = createGroup("冲突写班2");

        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "branchId": 1,
                                  "classGroupId": %d,
                                  "teacherId": 1101,
                                  "startAt": "2026-07-01T09:00:00",
                                  "endAt": "2026-07-01T10:30:00"
                                }
                                """
                                        .formatted(classGroupId)))
                .andExpect(status().isOk());
    }

    @Test
    void create_withTeacherConflict_returns409() throws Exception {
        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "branchId": 1,
                                  "classGroupId": %d,
                                  "teacherId": 1101,
                                  "startAt": "2026-07-01T09:30:00",
                                  "endAt": "2026-07-01T11:00:00"
                                }
                                """
                                        .formatted(otherGroupId)))
                .andExpect(status().isConflict());
    }

    private long createGroup(String name) throws Exception {
        long courseId = objectMapper
                .readTree(mockMvc.perform(post("/api/courses")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"" + name + "-" + UUID.randomUUID()
                                        + "\",\"lessonMinutes\":60}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();
        return objectMapper
                .readTree(mockMvc.perform(post("/api/class-groups")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"branchId":1,"name":"%s","courseId":%d,"capacity":10}
                                        """
                                                .formatted(name, courseId)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();
    }
}
