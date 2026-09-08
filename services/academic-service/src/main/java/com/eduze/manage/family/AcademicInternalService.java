package com.eduze.manage.family;

import com.eduze.platform.runtime.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AcademicInternalService {
    public record LessonValidation(String groupId, String branchId, List<String> lessonIds) {}

    private final AcademicAccess access;
    private final JdbcTemplate jdbc;
    private final Environment env;

    public Map<String, Object> notificationAccess(String lessonId, String studentId) {
        String tenant = env.getRequiredProperty("eduze.tenant.default-id");
        var rows =
                jdbc.queryForList(
                        "SELECT l.version,l.start_at,l.status,EXISTS(SELECT 1 FROM t_lesson_student s WHERE s.tenant_id=l.tenant_id AND s.lesson_id=l.id AND s.student_id=? AND s.status='BOOKED' AND s.deleted_at=0 AND s.removed_at IS NULL) AS in_roster FROM t_lesson l WHERE l.tenant_id=? AND l.id=? AND l.deleted_at=0",
                        studentId,
                        tenant,
                        lessonId);
        if (rows.isEmpty()) return Map.of("allowed", false, "revision", 0, "startAt", "");
        var row = rows.get(0);
        Object raw = row.get("start_at");
        var start =
                raw instanceof java.sql.Timestamp t
                        ? t.toLocalDateTime()
                        : (java.time.LocalDateTime) raw;
        return Map.of(
                "allowed",
                "SCHEDULED".equals(row.get("status"))
                        && ((Number) row.get("in_roster")).intValue() == 1,
                "revision",
                row.get("version"),
                "startAt",
                start.atZone(java.time.ZoneId.of("Asia/Shanghai")).toOffsetDateTime().toString());
    }

    public Map<String, Boolean> validate(LessonValidation request) {
        var group = access.group(request.groupId());
        if (!Objects.equals(group.branchId(), request.branchId())
                || request.lessonIds() == null
                || request.lessonIds().isEmpty()
                || request.lessonIds().size() > 100) throw new PlatformException(400, "课次与班级范围不匹配");
        for (String id : request.lessonIds().stream().distinct().toList()) {
            Integer count =
                    jdbc.queryForObject(
                            "SELECT COUNT(*) FROM t_lesson WHERE tenant_id=? AND id=? AND branch_id=? AND class_group_id=? AND deleted_at=0 AND status<>'CANCELLED'",
                            Integer.class,
                            Actors.current().tenantId(),
                            id,
                            request.branchId(),
                            request.groupId());
            if (count != 1) throw new PlatformException(400, "课次不属于班级或已取消");
        }
        return Map.of("allowed", true);
    }

    public List<Map<String, Object>> families(String id) {
        var s = access.student(id);
        access.requireStaff(s.branchId(), "student:read");
        return jdbc.query(
                "SELECT user_id,student_id,branch_id FROM family_binding WHERE tenant_id=? AND student_id=? AND status='ACTIVE'",
                (rs, n) ->
                        Map.<String, Object>of(
                                "userId",
                                rs.getString(1),
                                "studentId",
                                rs.getString(2),
                                "branchId",
                                rs.getString(3)),
                Actors.current().tenantId(),
                id);
    }

    public Map<String, Object> recipients(String id) {
        String tenant = env.getRequiredProperty("eduze.tenant.default-id");
        var rows =
                jdbc.queryForList(
                        "SELECT branch_id FROM t_student WHERE tenant_id=? AND id=? AND deleted_at=0",
                        tenant,
                        id);
        if (rows.isEmpty()) throw new PlatformException(404, "学员不存在");
        var recipients =
                jdbc.query(
                        "SELECT user_id FROM family_binding WHERE tenant_id=? AND student_id=? AND status='ACTIVE'",
                        (rs, n) -> Map.of("userId", rs.getString(1)),
                        tenant,
                        id);
        return Map.of(
                "tenantId",
                tenant,
                "branchId",
                rows.get(0).get("branch_id").toString(),
                "recipients",
                recipients);
    }
}
