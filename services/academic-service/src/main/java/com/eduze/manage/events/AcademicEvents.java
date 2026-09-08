package com.eduze.manage.events;

import com.eduze.manage.attendance.domain.LeaveRequest;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.platform.runtime.Outbox;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AcademicEvents {
    private final Outbox outbox;
    private final JdbcTemplate jdbc;

    public void rosterChanged(Long tenantId, Long lessonId) {
        jdbc.update(
                "UPDATE t_lesson SET version=version+1,updated_at=NOW(3) WHERE tenant_id=? AND id=? AND deleted_at=0",
                tenantId,
                lessonId);
        // JDBC mutations do not clear MyBatis' session cache; read the locked row directly.
        Lesson current =
                jdbc.queryForObject(
                        "SELECT id,tenant_id,branch_id,version,status,start_at,end_at FROM t_lesson WHERE tenant_id=? AND id=? AND deleted_at=0 FOR UPDATE",
                        (rs, rowNumber) -> {
                            Lesson row = new Lesson();
                            row.setId(rs.getLong("id"));
                            row.setTenantId(rs.getLong("tenant_id"));
                            row.setBranchId(rs.getLong("branch_id"));
                            row.setVersion(rs.getInt("version"));
                            row.setStatus(rs.getString("status"));
                            row.setStartAt(rs.getTimestamp("start_at").toLocalDateTime());
                            row.setEndAt(rs.getTimestamp("end_at").toLocalDateTime());
                            return row;
                        },
                        tenantId,
                        lessonId);
        lessonChanged(current);
    }

    public void lessonChanged(Lesson lesson) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("lessonId", lesson.getId().toString());
        payload.put("revision", lesson.getVersion() == null ? 1 : lesson.getVersion());
        payload.put("status", lesson.getStatus());
        payload.put(
                "startTime",
                lesson.getStartAt()
                        .atZone(ZoneId.of("Asia/Shanghai"))
                        .toOffsetDateTime()
                        .toString());
        payload.put(
                "endTime",
                lesson.getEndAt().atZone(ZoneId.of("Asia/Shanghai")).toOffsetDateTime().toString());
        payload.put(
                "studentIds",
                jdbc.queryForList(
                        "SELECT student_id FROM t_lesson_student WHERE tenant_id=? AND lesson_id=? AND deleted_at=0 AND status='BOOKED'",
                        String.class,
                        lesson.getTenantId(),
                        lesson.getId()));
        outbox.enqueue(
                "notification",
                "academic.lesson-changed",
                lesson.getTenantId().toString(),
                lesson.getBranchId().toString(),
                lesson.getId().toString(),
                payload);
    }

    public void leaveApproved(LeaveRequest leave) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("leaveId", leave.getId().toString());
        payload.put("studentId", leave.getStudentId().toString());
        payload.put(
                "lessonId", leave.getLessonId() == null ? null : leave.getLessonId().toString());
        payload.put("status", "APPROVED");
        payload.put("revision", leave.getVersion() == null ? 1 : leave.getVersion());
        outbox.enqueue(
                "notification",
                "academic.leave-approved",
                leave.getTenantId().toString(),
                leave.getBranchId().toString(),
                leave.getId().toString(),
                payload);
    }
}
