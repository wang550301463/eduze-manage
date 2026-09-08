package com.eduze.manage.family;

import com.eduze.platform.runtime.Actor;
import com.eduze.platform.runtime.Actors;
import com.eduze.platform.runtime.PlatformException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Authoritative access checks use local live relations, never a cached family projection. */
@Service
@RequiredArgsConstructor
public class AcademicAccess {
    private final JdbcTemplate jdbc;

    public record StudentView(String id, String name, String branchId) {}

    public record Access(boolean allowed, String branchId) {}

    public StudentView student(String id) {
        Actor actor = Actors.current();
        List<StudentView> rows =
                jdbc.query(
                        "SELECT id,name,branch_id FROM t_student WHERE tenant_id=? AND id=? AND deleted_at=0",
                        (rs, n) ->
                                new StudentView(
                                        rs.getString("id"),
                                        rs.getString("name"),
                                        rs.getString("branch_id")),
                        actor.tenantId(),
                        id);
        if (rows.isEmpty()) {
            throw new PlatformException(404, "学员不存在");
        }
        StudentView student = rows.get(0);
        if (isFamily(id, actor.userId()) || staffStudent(actor, student)) {
            return student;
        }
        throw new PlatformException(403, "无权访问该学员");
    }

    public boolean isFamily(String studentId, String userId) {
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM family_binding WHERE tenant_id=? AND student_id=? AND user_id=? AND status='ACTIVE'",
                        Integer.class,
                        Actors.current().tenantId(),
                        studentId,
                        userId)
                > 0;
    }

    public void requireFamily(String id) {
        if (!isFamily(id, Actors.current().userId())) {
            throw new PlatformException(403, "尚未获准访问该孩子");
        }
        student(id);
    }

    public void requireStaff(String branchId, String permission) {
        Actor actor = Actors.current();
        if (!actor.isStaff()) {
            throw new PlatformException(403, "需要员工身份");
        }
        actor.requireBranch(branchId);
        actor.requirePermission(permission);
    }

    private boolean staffStudent(Actor actor, StudentView student) {
        if (!actor.isStaff()) {
            return false;
        }
        actor.requireBranch(student.branchId());
        if (actor.isSuperAdmin()
                || actor.roles().contains("PRINCIPAL")
                || actor.roles().contains("ADMIN")
                || actor.roles().contains("ADVISOR")
                || actor.roles().contains("FRONT_DESK")
                || actor.roles().contains("RECEPTIONIST")) {
            return true;
        }
        if (!actor.roles().contains("TEACHER")) {
            return false;
        }
        return jdbc.queryForObject(
                        """
                SELECT COUNT(*) FROM t_student s WHERE s.tenant_id=? AND s.id=? AND s.deleted_at=0 AND
                (s.mentor_teacher_id=? OR EXISTS (SELECT 1 FROM t_lesson_student ls JOIN t_lesson l ON l.id=ls.lesson_id
                 AND l.tenant_id=ls.tenant_id WHERE ls.tenant_id=s.tenant_id AND ls.student_id=s.id
                 AND ls.deleted_at=0 AND ls.status='BOOKED' AND l.deleted_at=0 AND l.teacher_id=?))
                """,
                        Integer.class,
                        actor.tenantId(),
                        student.id(),
                        actor.userId(),
                        actor.userId())
                > 0;
    }

    public Access group(String id) {
        Actor actor = Actors.current();
        List<Map<String, Object>> rows =
                jdbc.queryForList(
                        "SELECT branch_id,head_teacher_id FROM t_class_group WHERE tenant_id=? AND id=? AND deleted_at=0",
                        actor.tenantId(),
                        id);
        if (rows.isEmpty()) {
            throw new PlatformException(404, "班级不存在");
        }
        String branchId = rows.get(0).get("branch_id").toString();
        requireStaff(branchId, "student:read");
        if (actor.roles().contains("TEACHER")
                && !actor.isSuperAdmin()
                && !actor.roles().contains("PRINCIPAL")
                && !actor.userId().equals(String.valueOf(rows.get(0).get("head_teacher_id")))) {
            Integer count =
                    jdbc.queryForObject(
                            "SELECT COUNT(*) FROM t_lesson WHERE tenant_id=? AND class_group_id=? AND teacher_id=? AND deleted_at=0",
                            Integer.class,
                            actor.tenantId(),
                            id,
                            actor.userId());
            if (count == 0) {
                throw new PlatformException(403, "未获准教授该班级");
            }
        }
        return new Access(true, branchId);
    }

    public List<StudentView> roster(String groupId) {
        group(groupId);
        return jdbc.query(
                """
                SELECT s.id,s.name,s.branch_id FROM t_student s JOIN t_student_class_group g ON g.student_id=s.id
                AND g.tenant_id=s.tenant_id WHERE s.tenant_id=? AND g.class_group_id=? AND s.deleted_at=0
                AND g.deleted_at=0 AND g.left_at IS NULL ORDER BY s.id
                """,
                (rs, n) ->
                        new StudentView(
                                rs.getString("id"),
                                rs.getString("name"),
                                rs.getString("branch_id")),
                Actors.current().tenantId(),
                groupId);
    }
}
