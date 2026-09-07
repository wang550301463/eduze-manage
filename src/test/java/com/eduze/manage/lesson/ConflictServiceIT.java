package com.eduze.manage.lesson;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.lesson.dto.ConflictReport;
import com.eduze.manage.lesson.service.ConflictService;
import com.eduze.manage.lesson.service.ConflictService.LessonDraft;
import com.eduze.manage.support.AbstractApiIT;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class ConflictServiceIT extends AbstractApiIT {

    @Autowired
    private ConflictService conflictService;

    private long classGroupId;
    private long teacherId = 1001L;

    @BeforeEach
    void seed() throws Exception {
        String token = adminToken();
        long courseId = objectMapper
                .readTree(mockMvc.perform(post("/api/courses")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"冲突课\",\"lessonMinutes\":60}"))
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
                                        {"branchId":1,"name":"冲突班","courseId":%d,"capacity":10,"headTeacherId":1001}
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
                                  "teacherId": 1001,
                                  "startAt": "2026-06-01T09:00:00",
                                  "endAt": "2026-06-01T10:30:00"
                                }
                                """
                                        .formatted(classGroupId)))
                .andExpect(status().isOk());
    }

    @Test
    void check_detectsTeacherConflict() {
        ConflictReport none = conflictService.check(new LessonDraft(
                null, 1L, classGroupId, null, teacherId,
                LocalDateTime.of(2026, 6, 10, 9, 0),
                LocalDateTime.of(2026, 6, 10, 10, 0)));
        assertFalse(none.isHasConflict());

        ConflictReport conflict = conflictService.check(new LessonDraft(
                null, 1L, classGroupId, null, teacherId,
                LocalDateTime.of(2026, 6, 1, 9, 30),
                LocalDateTime.of(2026, 6, 1, 10, 0)));
        assertTrue(conflict.isHasConflict());
        assertNotNull(conflict.getTeacher());
        assertNull(conflict.getClassRoom());
    }
}
