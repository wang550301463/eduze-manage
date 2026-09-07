package com.eduze.manage.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.common.util.IdGenerator;
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
class PickupServiceIT extends AbstractITContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void login() throws Exception {
        token = AuthTestSupport.loginAdmin(mockMvc, objectMapper);
    }

    @Test
    void checkOut_strangerGuardianMarkedAbnormal() throws Exception {
        LocalDateTime start = LocalDate.now().atTime(14, 0);
        long lessonId = AttendanceFixture.seedLessonWithStudent(jdbcTemplate, start, start.plusHours(1));
        long studentId = AttendanceFixture.studentIdForLesson(jdbcTemplate, lessonId);
        long guardianId = AttendanceFixture.seedGuardian(jdbcTemplate, studentId, true);

        var checkIn = mockMvc.perform(post("/api/attendance/check-in")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                                {"lessonId":%d,"studentId":%d,"method":"manual","guardianId":%d}
                                """,
                                lessonId, studentId, guardianId)))
                .andExpect(status().isOk())
                .andReturn();
        long attendanceId = objectMapper
                .readTree(checkIn.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        long strangerId = IdGenerator.nextId();
        jdbcTemplate.update(
                "INSERT INTO t_guardian (id, tenant_id, name, phone, can_pickup) VALUES (?, 1, '陌生人', ?, 1)",
                strangerId,
                "13900001111");

        mockMvc.perform(post("/api/attendance/check-out")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                                {"attendanceId":%d,"guardianId":%d}
                                """,
                                attendanceId, strangerId)))
                .andExpect(status().isOk());

        Integer abnormal = jdbcTemplate.queryForObject(
                """
                SELECT is_abnormal FROM t_pickup_record
                WHERE attendance_id = ? AND event_type = 'out' ORDER BY id DESC LIMIT 1
                """,
                Integer.class,
                attendanceId);
        assertEquals(1, abnormal);
    }
}
