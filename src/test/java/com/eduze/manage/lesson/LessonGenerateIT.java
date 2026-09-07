package com.eduze.manage.lesson;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class LessonGenerateIT extends AbstractApiIT {

    private static final AtomicInteger SLOT = new AtomicInteger(700);

    private String token;
    private long classGroupId;
    private int dayOfWeek;
    private int startMinute;
    private int endMinute;
    private long teacherId = 1103L;

    @BeforeEach
    void seed() throws Exception {
        token = adminToken();
        dayOfWeek = 4; // Thursday — seed 1103 用的是周五/周六
        startMinute = SLOT.getAndAdd(70);
        endMinute = startMinute + 60;

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

        var create = mockMvc.perform(post("/api/class-groups")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "branchId":1,
                                  "name":"批量班-%s",
                                  "courseId":%d,
                                  "nestedAvailability":{
                                    "teacherId":%d,
                                    "branchId":1,
                                    "dayOfWeek":%d,
                                    "startMinute":%d,
                                    "endMinute":%d,
                                    "capacity":20,
                                    "validFrom":"2026-01-01",
                                    "status":1
                                  }
                                }
                                """
                                        .formatted(
                                                java.util.UUID.randomUUID(),
                                                courseId,
                                                teacherId,
                                                dayOfWeek,
                                                startMinute,
                                                endMinute)))
                .andExpect(status().isOk())
                .andReturn();

        classGroupId = objectMapper
                .readTree(create.getResponse().getContentAsString())
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
                                  "teacherIds": [%d]
                                }
                                """
                                        .formatted(teacherId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generated").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void bulkGenerate_withExistingConflict_returns409() throws Exception {
        // 选 2026-08-06（周四）与本组成绑定窗口重叠
        LocalDate lessonDate = LocalDate.of(2026, 8, 6);
        LocalDateTime overlapStart = lessonDate.atStartOfDay().plusMinutes(startMinute + 15);
        LocalDateTime overlapEnd = lessonDate.atStartOfDay().plusMinutes(endMinute + 15);

        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "branchId": 1,
                                  "classGroupId": %d,
                                  "teacherId": %d,
                                  "startAt": "%s",
                                  "endAt": "%s"
                                }
                                """
                                        .formatted(classGroupId, teacherId, overlapStart, overlapEnd)))
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
                                  "teacherIds": [%d]
                                }
                                """
                                        .formatted(teacherId)))
                .andExpect(status().isConflict());
    }
}
