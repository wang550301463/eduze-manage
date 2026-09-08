package com.eduze.manage.integration;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.eduze.platform.runtime.PlatformException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrialReservationService {
    private final JdbcTemplate jdbc;
    private final IntegrationScope scope;
    private final RosterCapacity capacity;
    private final com.eduze.manage.course.service.ClassGroupMemberService members;
    private final com.eduze.platform.runtime.Outbox outbox;
    private final com.eduze.manage.events.AcademicEvents events;

    public record Request(
            String reservationId,
            String tenantId,
            String branchId,
            String sessionId,
            String studentName,
            String contactPhone) {}

    public record Enrollment(String enquiryId, String enrollmentId, String classGroupId) {}

    public record State(String reservationId, String status, String studentId) {}

    @Transactional
    public State reserve(Request request) {
        scope.requireTenant(request.tenantId());
        scope.requireId(request.reservationId());
        if (request.studentName() == null
                || request.studentName().isBlank()
                || request.studentName().length() > 64
                || request.contactPhone() == null
                || !request.contactPhone().matches("[+0-9 -]{6,32}"))
            throw new PlatformException(400, "试听学员信息无效");
        var lesson = capacity.lock(request.tenantId(), request.sessionId());
        if (!lesson.get("branch_id").toString().equals(request.branchId()))
            throw new PlatformException(403, "课次校区不匹配");
        var existing =
                jdbc.queryForList(
                        "SELECT student_id,status,request_hash FROM academic_trial_reservation WHERE tenant_id=? AND reservation_id=?",
                        request.tenantId(),
                        request.reservationId());
        String hash = hash(request);
        if (!existing.isEmpty()) {
            var row = existing.get(0);
            if (!hash.equals(row.get("request_hash")))
                throw new PlatformException(409, "预约 ID 对应不同内容");
            return new State(
                    request.reservationId(),
                    row.get("status").toString(),
                    row.get("student_id").toString());
        }
        if (!"SCHEDULED".equals(lesson.get("status"))
                || start(lesson.get("start_at"))
                        .atZone(java.time.ZoneId.of("Asia/Shanghai"))
                        .toInstant()
                        .isBefore(Instant.now())) throw new PlatformException(409, "课次不可预约");
        capacity.requireSpace(request.tenantId(), request.sessionId(), lesson);
        String student = String.valueOf(IdWorker.getId());
        jdbc.update(
                "INSERT INTO t_student(id,tenant_id,branch_id,enroll_no,name,emergency_phone,mentor_teacher_id,status) VALUES(?,?,?,?,?,?,?,1)",
                student,
                request.tenantId(),
                request.branchId(),
                "TRIAL-" + student,
                request.studentName(),
                request.contactPhone(),
                lesson.get("teacher_id"));
        jdbc.update(
                "INSERT INTO t_lesson_student(id,tenant_id,branch_id,lesson_id,student_id,source,status) VALUES(?,?,?,?,?,'TRIAL','BOOKED')",
                IdWorker.getId(),
                request.tenantId(),
                request.branchId(),
                request.sessionId(),
                student);
        jdbc.update(
                "INSERT INTO academic_trial_reservation(reservation_id,tenant_id,branch_id,session_id,student_id,request_hash,status) VALUES(?,?,?,?,?,?,'CONFIRMED')",
                request.reservationId(),
                request.tenantId(),
                request.branchId(),
                request.sessionId(),
                student,
                hash);
        events.rosterChanged(Long.valueOf(request.tenantId()), Long.valueOf(request.sessionId()));
        return new State(request.reservationId(), "CONFIRMED", student);
    }

    public State get(String id) {
        scope.requireId(id);
        var rows =
                jdbc.query(
                        "SELECT reservation_id,status,student_id FROM academic_trial_reservation WHERE tenant_id=? AND reservation_id=?",
                        (rs, n) -> new State(rs.getString(1), rs.getString(2), rs.getString(3)),
                        scope.tenant(),
                        id);
        if (rows.isEmpty()) throw new PlatformException(404, "预约不存在");
        return rows.get(0);
    }

    @Transactional
    public State cancel(String id) {
        var rows =
                jdbc.queryForList(
                        "SELECT session_id FROM academic_trial_reservation WHERE tenant_id=? AND reservation_id=?",
                        scope.tenant(),
                        id);
        if (rows.isEmpty()) throw new PlatformException(404, "预约不存在");
        capacity.lock(scope.tenant(), rows.get(0).get("session_id").toString());
        State state =
                jdbc.queryForObject(
                        "SELECT reservation_id,status,student_id FROM academic_trial_reservation WHERE tenant_id=? AND reservation_id=? FOR UPDATE",
                        (rs, n) -> new State(rs.getString(1), rs.getString(2), rs.getString(3)),
                        scope.tenant(),
                        id);
        if ("CANCELLED".equals(state.status())) return state;
        if ("ENROLLED".equals(state.status())) throw new PlatformException(409, "已正式报名的预约不可取消");
        if (jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_attendance WHERE tenant_id=? AND lesson_id=? AND student_id=? AND deleted_at=0",
                        Integer.class,
                        scope.tenant(),
                        rows.get(0).get("session_id"),
                        state.studentId())
                > 0) throw new PlatformException(409, "已到课的预约不可取消");
        jdbc.update(
                "UPDATE t_lesson_student SET status='CANCELLED',removed_at=NOW(3) WHERE tenant_id=? AND lesson_id=? AND student_id=?",
                scope.tenant(),
                rows.get(0).get("session_id"),
                state.studentId());
        jdbc.update(
                "UPDATE academic_trial_reservation SET status='CANCELLED',updated_at=NOW(3) WHERE tenant_id=? AND reservation_id=?",
                scope.tenant(),
                id);
        events.rosterChanged(
                Long.valueOf(scope.tenant()),
                Long.valueOf(rows.get(0).get("session_id").toString()));
        return new State(id, "CANCELLED", state.studentId());
    }

    @Transactional
    public State enroll(String id, Enrollment request) {
        scope.requireId(id);
        scope.requireId(request.enquiryId());
        scope.requireId(request.enrollmentId());
        if (request.classGroupId() == null || !request.classGroupId().matches("[0-9]{1,18}"))
            throw new PlatformException(400, "班级 ID 无效");
        var actor = com.eduze.platform.runtime.Actors.current();
        scope.requireTenant(actor.tenantId());
        if (!actor.isStaff()) throw new PlatformException(403, "需要员工身份");
        actor.requirePermission("classgroup:assign");
        var rows =
                jdbc.queryForList(
                        "SELECT branch_id,session_id FROM academic_trial_reservation WHERE tenant_id=? AND reservation_id=?",
                        scope.tenant(),
                        id);
        if (rows.isEmpty()) throw new PlatformException(404, "预约不存在");
        var trial = rows.get(0);
        String branch = trial.get("branch_id").toString();
        actor.requireBranch(branch);
        var groups =
                jdbc.queryForList(
                        "SELECT branch_id FROM t_class_group WHERE tenant_id=? AND id=? AND status=1 AND deleted_at=0 FOR UPDATE",
                        scope.tenant(),
                        request.classGroupId());
        if (groups.isEmpty()) throw new PlatformException(404, "班级不存在");
        if (!branch.equals(groups.get(0).get("branch_id").toString()))
            throw new PlatformException(403, "班级与试听校区不同");
        capacity.lock(scope.tenant(), trial.get("session_id").toString());
        var row =
                jdbc.queryForList(
                                "SELECT student_id,status,enquiry_id,enrollment_id,enrollment_group_id FROM academic_trial_reservation WHERE tenant_id=? AND reservation_id=? FOR UPDATE",
                                scope.tenant(),
                                id)
                        .get(0);
        String student = row.get("student_id").toString();
        if ("ENROLLED".equals(row.get("status"))) {
            if (!request.enrollmentId().equals(row.get("enrollment_id"))
                    || !request.enquiryId().equals(row.get("enquiry_id"))
                    || !request.classGroupId().equals(row.get("enrollment_group_id").toString()))
                throw new PlatformException(409, "报名 ID 对应不同内容");
            return new State(id, "ENROLLED", student);
        }
        if (!"CONFIRMED".equals(row.get("status"))) throw new PlatformException(409, "预约当前不可报名");
        var addition = new com.eduze.manage.course.dto.AddMembersRequest();
        addition.setStudentIds(java.util.List.of(Long.valueOf(student)));
        members.addMembers(Long.valueOf(request.classGroupId()), addition);
        jdbc.update(
                "UPDATE academic_trial_reservation SET status='ENROLLED',enquiry_id=?,enrollment_id=?,enrollment_group_id=?,updated_at=NOW(3) WHERE tenant_id=? AND reservation_id=?",
                request.enquiryId(),
                request.enrollmentId(),
                request.classGroupId(),
                scope.tenant(),
                id);
        outbox.enqueue(
                "engagement",
                "academic.enrollment-confirmed",
                scope.tenant(),
                branch,
                request.enrollmentId(),
                Map.of("enquiryId", request.enquiryId(), "enrollmentId", request.enrollmentId()));
        return new State(id, "ENROLLED", student);
    }

    private java.time.LocalDateTime start(Object value) {
        return value instanceof Timestamp timestamp
                ? timestamp.toLocalDateTime()
                : (java.time.LocalDateTime) value;
    }

    private String hash(Request request) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            (request.tenantId()
                                                            + "|"
                                                            + request.branchId()
                                                            + "|"
                                                            + request.sessionId()
                                                            + "|"
                                                            + request.studentName()
                                                            + "|"
                                                            + request.contactPhone())
                                                    .getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
