package com.eduze.manage.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.attendance.domain.AttendanceStatus;
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
class LeaveServiceIT extends AbstractITContainerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void login() throws Exception {
        token = AuthTestSupport.loginAdmin(mockMvc, objectMapper);
    }

    @Test
    void approveLeave_updatesExistingAttendance() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atTime(9, 30);
        long lessonId =
                AttendanceFixture.seedLessonWithStudent(jdbcTemplate, start, start.plusHours(1));
        long studentId = AttendanceFixture.studentIdForLesson(jdbcTemplate, lessonId);
        long guardianId = AttendanceFixture.seedGuardian(jdbcTemplate, studentId, true);

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
                .andExpect(status().isOk());

        var leaveResult =
                mockMvc.perform(
                                post("/api/leaves")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                String.format(
                                                        """
                                {"studentId":%d,"leaveStartDate":"%s","leaveEndDate":"%s","reason":"生病"}
                                """,
                                                        studentId, today, today)))
                        .andExpect(status().isOk())
                        .andReturn();
        long leaveId =
                objectMapper
                        .readTree(leaveResult.getResponse().getContentAsString())
                        .path("data")
                        .path("id")
                        .asLong();

        mockMvc.perform(
                        post("/api/leaves/" + leaveId + "/approve")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(2));

        Integer status =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM t_attendance WHERE lesson_id = ? AND student_id = ?",
                        Integer.class,
                        lessonId,
                        studentId);
        assertEquals(AttendanceStatus.LEAVE.getCode(), status);
    }
}
