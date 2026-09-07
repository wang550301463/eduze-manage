package com.eduze.manage.lesson;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class LessonGenerateIT extends AbstractApiIT {

    private String token;
    private long classGroupId;

    @BeforeEach
    void seed() throws Exception {
        token = adminToken();
        long courseId = objectMapper
                .readTree(mockMvc.perform(post("/api/courses")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"批量课-" + java.util.UUID.randomUUID()
                                        + "\",\"lessonMinutes\":90}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();

        classGroupId = objectMapper
                .readTree(mockMvc.perform(post("/api/class-groups")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"branchId":1,"name":"批量班-%s","courseId":%d,"capacity":20}
                                        """
                                                .formatted(java.util.UUID.randomUUID(), courseId)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();
    }

    @Test
    void bulkGenerate_createsLessons() throws Exception {
        mockMvc.perform(post("/api/lessons/bulk-generate")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "fromDate": "2026-05-13",
                                  "weeks": 2,
                                  "branchId": 1,
                                  "teacherIds": [1101]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generated").value(4));
    }

    @Test
    void bulkGenerate_withExistingConflict_returns409() throws Exception {
        // 使用独立周次，避免与 bulkGenerate_createsLessons 写入的 5 月课次互撞
        // 与模板周六 09:00-10:30 重叠但不完全相同，避免被幂等 skip
        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "branchId": 1,
                                  "classGroupId": %d,
                                  "teacherId": 1101,
                                  "startAt": "2026-08-08T09:15:00",
                                  "endAt": "2026-08-08T10:45:00"
                                }
                                """
                                        .formatted(classGroupId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lessons/bulk-generate")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "fromDate": "2026-08-03",
                                  "weeks": 1,
                                  "branchId": 1,
                                  "teacherIds": [1101]
                                }
                                """))
                .andExpect(status().isConflict());
    }
}
