package com.eduze.manage.support;

import com.eduze.manage.common.util.IdGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;

public final class AttendanceFixture {

    private AttendanceFixture() {}

    public static long seedLessonWithStudent(
            JdbcTemplate jdbc, LocalDateTime start, LocalDateTime end) {
        long branchId = 1L;
        long courseId = IdGenerator.nextId();
        jdbc.update(
                "INSERT INTO t_course (id, tenant_id, name) VALUES (?, 1, ?)",
                courseId,
                "测试课程-" + courseId);
        long classGroupId = IdGenerator.nextId();
        jdbc.update(
                """
                INSERT INTO t_class_group (id, tenant_id, branch_id, name, course_id, capacity, status)
                VALUES (?, 1, ?, ?, ?, 20, 1)
                """,
                classGroupId,
                branchId,
                "测试班-" + classGroupId,
                courseId);
        long studentId = IdGenerator.nextId();
        jdbc.update(
                """
                INSERT INTO t_student (id, tenant_id, branch_id, enroll_no, name, status)
                VALUES (?, 1, ?, ?, ?, 1)
                """,
                studentId,
                branchId,
                "E" + studentId,
                "测试学员");
        jdbc.update(
                """
                INSERT INTO t_student_class_group (id, tenant_id, student_id, class_group_id, joined_at)
                VALUES (?, 1, ?, ?, NOW(3))
                """,
                IdGenerator.nextId(),
                studentId,
                classGroupId);
        long lessonId = IdGenerator.nextId();
        jdbc.update(
                """
                INSERT INTO t_lesson (id, tenant_id, branch_id, class_group_id, start_at, end_at, status)
                VALUES (?, 1, ?, ?, ?, ?, 1)
                """,
                lessonId,
                branchId,
                classGroupId,
                start,
                end);
        return lessonId;
    }

    public static long seedGuardian(JdbcTemplate jdbc, long studentId, boolean canPickup) {
        long guardianId = IdGenerator.nextId();
        jdbc.update(
                """
                INSERT INTO t_guardian (id, tenant_id, name, phone, can_pickup, is_main_contact)
                VALUES (?, 1, '张家长', ?, ?, 1)
                """,
                guardianId,
                "138" + (guardianId % 100000000L),
                canPickup ? 1 : 0);
        jdbc.update(
                """
                INSERT INTO t_student_guardian_relation (id, tenant_id, student_id, guardian_id, relation)
                VALUES (?, 1, ?, ?, '父亲')
                """,
                IdGenerator.nextId(),
                studentId,
                guardianId);
        return guardianId;
    }

    public static long studentIdForLesson(JdbcTemplate jdbc, long lessonId) {
        return jdbc.queryForObject(
                """
                SELECT scg.student_id FROM t_lesson l
                JOIN t_student_class_group scg ON scg.class_group_id = l.class_group_id
                WHERE l.id = ? LIMIT 1
                """,
                Long.class,
                lessonId);
    }

    public static LocalDate today() {
        return LocalDate.now();
    }
}
