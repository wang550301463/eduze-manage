package com.eduze.manage.attendance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.support.AttendanceFixture;
import com.eduze.manage.support.AuthTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AttendanceControllerIT extends AbstractITContainerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void login() throws Exception {
        LocalDate today = LocalDate.now();
        jdbcTemplate.update(
                "DELETE FROM t_attendance WHERE lesson_id IN (SELECT id FROM t_lesson WHERE branch_id = 1 AND start_at >= ? AND start_at < ?)",
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay());
        jdbcTemplate.update(
                "DELETE FROM t_lesson WHERE branch_id = 1 AND start_at >= ? AND start_at < ?",
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay());
        token = AuthTestSupport.loginAdmin(mockMvc, objectMapper);
    }

    @Test
    void todayRoster_andCheckIn() throws Exception {
        LocalDateTime start = LocalDate.now().atTime(9, 0);
        long lessonId =
                AttendanceFixture.seedLessonWithStudent(jdbcTemplate, start, start.plusHours(2));
        long studentId = AttendanceFixture.studentIdForLesson(jdbcTemplate, lessonId);
        long guardianId = AttendanceFixture.seedGuardian(jdbcTemplate, studentId, true);

        mockMvc.perform(
                        get("/api/attendance/today")
                                .header("Authorization", "Bearer " + token)
                                .param("branchId", "1")
                                .param("period", "morning"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalExpected").value(1));

        mockMvc.perform(
                        post("/api/attendance/check-in")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        String.format(
                                                """
                                {"lessonId":%d,"studentId":%d,"method":"manual","guardianId":%d}
                                """,
                                                lessonId, studentId, guardianId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(2));

        mockMvc.perform(
                        post("/api/attendance/check-in")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        String.format(
                                                """
                                {"lessonId":%d,"studentId":%d,"method":"manual","guardianId":%d}
                                """,
                                                lessonId, studentId, guardianId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("学员已签到"));
    }

    @Test
    void qrCheckIn() throws Exception {
        LocalDateTime start = LocalDate.now().atTime(10, 0);
        long lessonId =
                AttendanceFixture.seedLessonWithStudent(jdbcTemplate, start, start.plusHours(1));
        long studentId = AttendanceFixture.studentIdForLesson(jdbcTemplate, lessonId);
        long guardianId = AttendanceFixture.seedGuardian(jdbcTemplate, studentId, true);

        var qrResult =
                mockMvc.perform(
                                post("/api/guardians/" + guardianId + "/qr")
                                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn();
        String body = qrResult.getResponse().getContentAsString();
        String qrCode = objectMapper.readTree(body).path("data").path("qrCode").asText();

        mockMvc.perform(
                        post("/api/attendance/check-in")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        String.format(
                                                """
                                {"lessonId":%d,"studentId":%d,"method":"qr","qrCode":"%s"}
                                """,
                                                lessonId, studentId, qrCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkInMethod").value("qr"));
    }
}
