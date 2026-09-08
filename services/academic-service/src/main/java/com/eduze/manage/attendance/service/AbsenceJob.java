package com.eduze.manage.attendance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.attendance.domain.Attendance;
import com.eduze.manage.attendance.domain.AttendanceStatus;
import com.eduze.manage.attendance.domain.CheckInMethod;
import com.eduze.manage.attendance.mapper.AttendanceMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AbsenceJob {

    private static final String LOCK_KEY = "academic:lock:absence-job";

    private final JdbcTemplate jdbcTemplate;
    private final AttendanceMapper attendanceMapper;
    private final AttendanceSupport attendanceSupport;
    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(cron = "0 */15 * * * *")
    public void scheduledRun() {
        runWithLock();
    }

    public void runWithLock() {
        Boolean acquired =
                stringRedisTemplate
                        .opsForValue()
                        .setIfAbsent(LOCK_KEY, "1", Duration.ofMinutes(10));
        if (Boolean.FALSE.equals(acquired)) {
            log.debug("Absence job skipped: lock held");
            return;
        }
        try {
            runInternal();
        } finally {
            stringRedisTemplate.delete(LOCK_KEY);
        }
    }

    public void runInternal() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        List<Map<String, Object>> lessons =
                jdbcTemplate.queryForList(
                        """
                SELECT id, tenant_id, branch_id, class_group_id, start_at, end_at
                FROM t_lesson
                WHERE deleted_at = 0 AND end_at <= ? AND end_at >= ?
                """,
                        cutoff,
                        cutoff.minusMinutes(20));

        for (Map<String, Object> lesson : lessons) {
            Long tenantId = ((Number) lesson.get("tenant_id")).longValue();
            TenantContext.setTenantId(tenantId);
            try {
                processLesson(lesson);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void processLesson(Map<String, Object> lesson) {
        Long lessonId = ((Number) lesson.get("id")).longValue();
        Long branchId = ((Number) lesson.get("branch_id")).longValue();
        LocalDateTime startAt = toLocalDateTime(lesson.get("start_at"));
        List<Long> studentIds =
                jdbcTemplate.queryForList(
                        "SELECT student_id FROM t_lesson_student WHERE tenant_id=? AND lesson_id=? AND deleted_at=0 AND status='BOOKED' AND removed_at IS NULL",
                        Long.class,
                        TenantContext.getTenantId(),
                        lessonId);

        for (Long studentId : studentIds) {
            Long exists =
                    attendanceMapper.selectCount(
                            Wrappers.<Attendance>lambdaQuery()
                                    .eq(Attendance::getLessonId, lessonId)
                                    .eq(Attendance::getStudentId, studentId));
            if (exists != null && exists > 0) {
                continue;
            }
            if (attendanceSupport.hasApprovedLeave(studentId, lessonId, startAt.toLocalDate())) {
                continue;
            }
            Attendance absent = new Attendance();
            absent.setTenantId(TenantContext.getTenantId());
            absent.setBranchId(branchId);
            absent.setLessonId(lessonId);
            absent.setStudentId(studentId);
            absent.setStatus(AttendanceStatus.ABSENT.getCode());
            absent.setCheckInMethod(CheckInMethod.AUTO);
            attendanceMapper.insert(absent);
        }
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        throw new IllegalArgumentException(
                "Unsupported datetime type: " + value.getClass().getName());
    }
}
