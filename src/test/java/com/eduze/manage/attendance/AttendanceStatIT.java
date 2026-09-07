package com.eduze.manage.attendance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.attendance.domain.AttendanceStatus;
import com.eduze.manage.common.util.IdGenerator;
import com.eduze.manage.support.AuthTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AttendanceStatIT extends AbstractITContainerTest {

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
    void studentStat_rate80Percent() throws Exception {
        long studentId = IdGenerator.nextId();
        jdbcTemplate.update(
                """
                INSERT INTO t_student (id, tenant_id, branch_id, enroll_no, name, status)
                VALUES (?, 1, 1, ?, '统计学员', 1)
                """,
                studentId,
                "STAT-" + studentId);
        LocalDate today = LocalDate.now();
        LocalDateTime base = today.atTime(10, 0);
        int[] statuses = {
            2, 2, 2, 2, 2, 2, 2, 2,
            AttendanceStatus.ABSENT.getCode(),
            AttendanceStatus.LEAVE.getCode()
        };
        for (int i = 0; i < statuses.length; i++) {
            long lessonId = IdGenerator.nextId();
            jdbcTemplate.update(
                    """
                    INSERT INTO t_lesson (id, tenant_id, branch_id, class_group_id, start_at, end_at, status)
                    VALUES (?, 1, 1, 1, ?, ?, 1)
                    """,
                    lessonId,
                    base.minusDays(i),
                    base.minusDays(i).plusHours(1));
            jdbcTemplate.update(
                    """
                    INSERT INTO t_attendance (
                        id, tenant_id, branch_id, lesson_id, student_id, status, check_in_at, check_in_method
                    ) VALUES (?, 1, 1, ?, ?, ?, ?, 'manual')
                    """,
                    IdGenerator.nextId(),
                    lessonId,
                    studentId,
                    statuses[i],
                    base.minusDays(i));
        }

        mockMvc.perform(get("/api/stats/attendance/student/" + studentId)
                        .header("Authorization", "Bearer " + token)
                        .param("from", today.minusDays(30).toString())
                        .param("to", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.present").value(8))
                .andExpect(jsonPath("$.data.rate").value(80.0));
    }
}
