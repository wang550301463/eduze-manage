package com.eduze.manage.family;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.eduze.platform.runtime.Actors;
import com.eduze.platform.runtime.Outbox;
import com.eduze.platform.runtime.PlatformException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FamilyService {
    private static final ZoneId LOCAL_ZONE = ZoneId.of("Asia/Shanghai");
    private final JdbcTemplate jdbc;
    private final AcademicAccess access;
    private final Outbox outbox;

    public record Invite(String id, String code, Instant expiresAt) {}

    public record Binding(String id, String studentId, String userId, String status) {}

    public record Schedule(
            String id,
            String startTime,
            String endTime,
            String status,
            String teacherId,
            String classGroupId) {}

    public record Leave(String id, String lessonId, String reason, String status) {}

    @Transactional
    public Invite invite(String studentId) {
        var student = access.student(studentId);
        access.requireStaff(student.branchId(), "guardian:write");
        String id = UUID.randomUUID().toString();
        String code =
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", "");
        Instant expiry = Instant.now().plusSeconds(86400);
        jdbc.update(
                "INSERT INTO family_invite(id,tenant_id,student_id,branch_id,code_hash,expires_at,created_by) VALUES(?,?,?,?,?,?,?)",
                id,
                Actors.current().tenantId(),
                studentId,
                student.branchId(),
                hash(code),
                Timestamp.from(expiry),
                Actors.current().userId());
        return new Invite(id, code, expiry);
    }

    @Transactional
    public Binding claim(String code) {
        if (code == null || !code.matches("[a-f0-9]{64}")) {
            throw new PlatformException(400, "邀请无效");
        }
        var actor = Actors.current();
        var rows =
                jdbc.queryForList(
                        "SELECT id,student_id,branch_id,expires_at,claimed_by FROM family_invite WHERE tenant_id=? AND code_hash=? FOR UPDATE",
                        actor.tenantId(),
                        hash(code));
        if (rows.isEmpty()) {
            throw new PlatformException(404, "邀请不存在");
        }
        var invite = rows.get(0);
        if (((Timestamp) invite.get("expires_at")).toInstant().isBefore(Instant.now())) {
            throw new PlatformException(409, "邀请已过期");
        }
        if (invite.get("claimed_by") != null
                && !invite.get("claimed_by").toString().equals(actor.userId())) {
            throw new PlatformException(409, "邀请已使用");
        }
        String studentId = invite.get("student_id").toString();
        var existing = bindingsFor(studentId, actor.userId());
        if (!existing.isEmpty() && !"REVOKED".equals(existing.get(0).status())) {
            return existing.get(0);
        }
        String id = existing.isEmpty() ? UUID.randomUUID().toString() : existing.get(0).id();
        if (existing.isEmpty()) {
            jdbc.update(
                    "INSERT INTO family_binding(id,tenant_id,student_id,branch_id,user_id,invite_id,status) VALUES(?,?,?,?,?,?,'PENDING')",
                    id,
                    actor.tenantId(),
                    studentId,
                    invite.get("branch_id"),
                    actor.userId(),
                    invite.get("id"));
        } else {
            if (invite.get("claimed_by") != null) {
                throw new PlatformException(409, "请使用新的邀请");
            }
            jdbc.update(
                    "UPDATE family_binding SET status='PENDING',invite_id=?,approved_by=NULL,approved_at=NULL,revoked_at=NULL WHERE id=? AND tenant_id=?",
                    invite.get("id"),
                    id,
                    actor.tenantId());
        }
        jdbc.update(
                "UPDATE family_invite SET claimed_by=? WHERE id=? AND tenant_id=?",
                actor.userId(),
                invite.get("id"),
                actor.tenantId());
        return new Binding(id, studentId, actor.userId(), "PENDING");
    }

    public List<Binding> bindings(String studentId) {
        var student = access.student(studentId);
        access.requireStaff(student.branchId(), "guardian:write");
        return jdbc.query(
                "SELECT id,student_id,user_id,status FROM family_binding WHERE tenant_id=? AND student_id=? ORDER BY created_at DESC",
                (rs, n) ->
                        new Binding(
                                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                Actors.current().tenantId(),
                studentId);
    }

    private List<Binding> bindingsFor(String studentId, String userId) {
        return jdbc.query(
                "SELECT id,student_id,user_id,status FROM family_binding WHERE tenant_id=? AND student_id=? AND user_id=?",
                (rs, n) ->
                        new Binding(
                                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                Actors.current().tenantId(),
                studentId,
                userId);
    }

    @Transactional
    public Binding approve(String id) {
        return change(id, true);
    }

    @Transactional
    public Binding revoke(String id) {
        return change(id, false);
    }

    private Binding change(String id, boolean approved) {
        var actor = Actors.current();
        var rows =
                jdbc.queryForList(
                        "SELECT student_id,branch_id,user_id,status FROM family_binding WHERE tenant_id=? AND id=? FOR UPDATE",
                        actor.tenantId(),
                        id);
        if (rows.isEmpty()) {
            throw new PlatformException(404, "绑定不存在");
        }
        var row = rows.get(0);
        access.requireStaff(row.get("branch_id").toString(), "guardian:write");
        String status = approved ? "ACTIVE" : "REVOKED";
        if (status.equals(row.get("status"))) {
            return new Binding(
                    id, row.get("student_id").toString(), row.get("user_id").toString(), status);
        }
        if (approved && !"PENDING".equals(row.get("status"))) {
            throw new PlatformException(409, "只能批准待确认的绑定");
        }
        jdbc.update(
                "UPDATE family_binding SET status=?,approved_by=?,approved_at=?,revoked_at=? WHERE id=? AND tenant_id=?",
                status,
                actor.userId(),
                Timestamp.from(Instant.now()),
                approved ? null : Timestamp.from(Instant.now()),
                id,
                actor.tenantId());
        outbox.enqueue(
                "notification",
                "academic.family-authorization-changed",
                actor.tenantId(),
                row.get("branch_id").toString(),
                id,
                Map.of(
                        "userId",
                        row.get("user_id").toString(),
                        "studentId",
                        row.get("student_id").toString(),
                        "status",
                        status));
        return new Binding(
                id, row.get("student_id").toString(), row.get("user_id").toString(), status);
    }

    public List<AcademicAccess.StudentView> children() {
        var actor = Actors.current();
        return jdbc.query(
                "SELECT s.id,s.name,s.branch_id FROM t_student s JOIN family_binding b ON b.student_id=s.id AND b.tenant_id=s.tenant_id WHERE b.tenant_id=? AND b.user_id=? AND b.status='ACTIVE' AND s.deleted_at=0 ORDER BY s.id",
                (rs, n) ->
                        new AcademicAccess.StudentView(
                                rs.getString(1), rs.getString(2), rs.getString(3)),
                actor.tenantId(),
                actor.userId());
    }

    public List<Schedule> schedule(String id, LocalDate from, LocalDate to) {
        access.requireFamily(id);
        if (to.isBefore(from) || from.plusDays(93).isBefore(to)) {
            throw new PlatformException(400, "查询区间必须在 93 天内");
        }
        return jdbc.query(
                """
                SELECT l.id,l.start_at,l.end_at,l.status,l.teacher_id,l.class_group_id FROM t_lesson l
                JOIN t_lesson_student s ON s.lesson_id=l.id AND s.tenant_id=l.tenant_id
                WHERE l.tenant_id=? AND s.student_id=? AND s.deleted_at=0 AND s.status='BOOKED'
                 AND l.deleted_at=0 AND l.start_at>=? AND l.start_at<? ORDER BY l.start_at
                """,
                (rs, n) ->
                        new Schedule(
                                rs.getString(1),
                                rs.getTimestamp(2)
                                        .toLocalDateTime()
                                        .atZone(LOCAL_ZONE)
                                        .toOffsetDateTime()
                                        .toString(),
                                rs.getTimestamp(3)
                                        .toLocalDateTime()
                                        .atZone(LOCAL_ZONE)
                                        .toOffsetDateTime()
                                        .toString(),
                                rs.getString(4),
                                rs.getString(5),
                                rs.getString(6)),
                Actors.current().tenantId(),
                id,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay());
    }

    public Map<String, Object> balance(String id) {
        access.requireFamily(id);
        Integer remaining =
                jdbc.queryForObject(
                        "SELECT COALESCE(SUM(remaining_lessons-frozen_lessons),0) FROM t_course_package WHERE tenant_id=? AND student_id=? AND deleted_at=0 AND (expire_date IS NULL OR expire_date>=CURRENT_DATE)",
                        Integer.class,
                        Actors.current().tenantId(),
                        id);
        return Map.of("remainingLessons", remaining == null ? 0 : remaining);
    }

    public List<Leave> leaves(String id) {
        access.requireFamily(id);
        return jdbc.query(
                "SELECT id,lesson_id,reason,status FROM t_leave_request WHERE tenant_id=? AND student_id=? AND deleted_at=0 ORDER BY created_at DESC LIMIT 200",
                (rs, n) ->
                        new Leave(
                                rs.getString(1),
                                rs.getString(2),
                                rs.getString(3),
                                leaveStatus(rs.getInt(4))),
                Actors.current().tenantId(),
                id);
    }

    @Transactional
    public Leave leave(String id, String lessonId, String reason, String requestKey) {
        access.requireFamily(id);
        if (reason == null
                || reason.isBlank()
                || reason.length() > 256
                || requestKey == null
                || !requestKey.matches("[A-Za-z0-9-]{8,64}")) {
            throw new PlatformException(400, "请填写原因并提供有效 Idempotency-Key");
        }
        var actor = Actors.current();
        var lessons =
                jdbc.queryForList(
                        "SELECT l.branch_id,l.start_at,l.status FROM t_lesson l JOIN t_lesson_student s ON s.lesson_id=l.id AND s.tenant_id=l.tenant_id WHERE l.tenant_id=? AND l.id=? AND s.student_id=? AND l.deleted_at=0 AND s.deleted_at=0 AND s.status='BOOKED' FOR UPDATE",
                        actor.tenantId(),
                        lessonId,
                        id);
        if (lessons.isEmpty()) {
            throw new PlatformException(403, "该课次不属于孩子");
        }
        var existing =
                jdbc.query(
                        "SELECT id,lesson_id,reason,status FROM t_leave_request WHERE tenant_id=? AND student_id=? AND family_request_key=?",
                        (rs, n) ->
                                new Leave(
                                        rs.getString(1),
                                        rs.getString(2),
                                        rs.getString(3),
                                        leaveStatus(rs.getInt(4))),
                        actor.tenantId(),
                        id,
                        requestKey);
        if (!existing.isEmpty()) {
            if (!existing.get(0).lessonId().equals(lessonId)
                    || !existing.get(0).reason().equals(reason)) {
                throw new PlatformException(409, "重复请求内容不一致");
            }
            return existing.get(0);
        }
        var lesson = lessons.get(0);
        var rawStart = lesson.get("start_at");
        var start =
                rawStart instanceof Timestamp timestamp
                        ? timestamp.toLocalDateTime()
                        : (java.time.LocalDateTime) rawStart;
        if (!"SCHEDULED".equals(lesson.get("status"))
                || start.atZone(LOCAL_ZONE).toInstant().isBefore(Instant.now())) {
            throw new PlatformException(409, "该课次已不能在线请假");
        }
        String leaveId = String.valueOf(IdWorker.getId());
        jdbc.update(
                "INSERT INTO t_leave_request(id,tenant_id,branch_id,student_id,lesson_id,leave_start_date,leave_end_date,reason,status,family_request_key,created_by) VALUES(?,?,?,?,?,?,?,?,1,?,?)",
                leaveId,
                actor.tenantId(),
                lesson.get("branch_id"),
                id,
                lessonId,
                start.toLocalDate(),
                start.toLocalDate(),
                reason,
                requestKey,
                actor.userId());
        return new Leave(leaveId, lessonId, reason, "PENDING");
    }

    private String leaveStatus(int code) {
        return switch (code) {
            case 1 -> "PENDING";
            case 2 -> "APPROVED";
            case 3 -> "REJECTED";
            default -> "UNKNOWN";
        };
    }

    private String hash(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
