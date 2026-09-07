package com.eduze.manage.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.attendance.domain.AttendanceStatus;
import com.eduze.manage.attendance.service.AbsenceJob;
import com.eduze.manage.common.util.IdGenerator;
import com.eduze.manage.support.AttendanceFixture;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class AbsenceJobIT extends AbstractITContainerTest {

    @Autowired
    private AbsenceJob absenceJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void runInternal_createsAbsentForEndedLesson() {
        LocalDateTime end = LocalDateTime.now().minusHours(1).minusMinutes(5);
        LocalDateTime start = end.minusHours(1);
        long lessonId = AttendanceFixture.seedLessonWithStudent(jdbcTemplate, start, end);
        long studentId = AttendanceFixture.studentIdForLesson(jdbcTemplate, lessonId);

        absenceJob.runInternal();

        Integer status = jdbcTemplate.queryForObject(
                "SELECT status FROM t_attendance WHERE lesson_id = ? AND student_id = ?",
                Integer.class,
                lessonId,
                studentId);
        assertEquals(AttendanceStatus.ABSENT.getCode(), status);

        long lessonId2 = AttendanceFixture.seedLessonWithStudent(
                jdbcTemplate, start.minusDays(1), end.minusDays(1));
        long studentId2 = AttendanceFixture.studentIdForLesson(jdbcTemplate, lessonId2);
        long leaveId = IdGenerator.nextId();
        jdbcTemplate.update(
                """
                INSERT INTO t_leave_request (
                    id, tenant_id, branch_id, student_id, leave_start_date, leave_end_date,
                    reason, status
                ) VALUES (?, 1, 1, ?, ?, ?, '病假', 2)
                """,
                leaveId,
                studentId2,
                start.toLocalDate(),
                end.toLocalDate());

        absenceJob.runInternal();
        Long absentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_attendance WHERE lesson_id = ? AND student_id = ? AND status = 4",
                Long.class,
                lessonId2,
                studentId2);
        assertTrue(absentCount == null || absentCount == 0);
    }

    @Test
    void runInternal_teacherCentricLesson_usesLessonStudentRoster() {
        LocalDateTime end = LocalDateTime.now().minusHours(1).minusMinutes(5);
        LocalDateTime start = end.minusHours(1);
        long branchId = 1L;
        long studentId = IdGenerator.nextId();
        jdbcTemplate.update(
                """
                INSERT INTO t_student (id, tenant_id, branch_id, enroll_no, name, status)
                VALUES (?, 1, ?, ?, ?, 1)
                """,
                studentId,
                branchId,
                "TC" + studentId,
                "老师中心学员");
        long lessonId = IdGenerator.nextId();
        jdbcTemplate.update(
                """
                INSERT INTO t_lesson (id, tenant_id, branch_id, class_group_id, teacher_id, start_at, end_at, status)
                VALUES (?, 1, ?, NULL, 1101, ?, ?, 'SCHEDULED')
                """,
                lessonId,
                branchId,
                start,
                end);
        jdbcTemplate.update(
                """
                INSERT INTO t_lesson_student (
                    id, tenant_id, branch_id, lesson_id, student_id, source, status
                ) VALUES (?, 1, ?, ?, ?, 'MANUAL', 'BOOKED')
                """,
                IdGenerator.nextId(),
                branchId,
                lessonId,
                studentId);

        absenceJob.runInternal();

        Integer status = jdbcTemplate.queryForObject(
                "SELECT status FROM t_attendance WHERE lesson_id = ? AND student_id = ?",
                Integer.class,
                lessonId,
                studentId);
        assertEquals(AttendanceStatus.ABSENT.getCode(), status);
    }
}
