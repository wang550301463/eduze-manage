package com.eduze.manage.student.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.domain.StudentLessonHourLedger;
import com.eduze.manage.student.dto.LessonHourAdjustRequest;
import com.eduze.manage.student.dto.StudentLessonHourLedgerResponse;
import com.eduze.manage.student.mapper.StudentLessonHourLedgerMapper;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 学员课时审计流水：只 INSERT，纠错走 VOID/ADJUST 新行。 */
@Service
@RequiredArgsConstructor
public class StudentLessonHourLedgerService {

    public static final String EVENT_ATTEND = "ATTEND";
    public static final String EVENT_ADJUST = "ADJUST";
    public static final String EVENT_VOID = "VOID";
    public static final String EVENT_ABSENT_DEDUCT = "ABSENT_DEDUCT";
    public static final String EVENT_MAKEUP = "MAKEUP";

    private final StudentLessonHourLedgerMapper ledgerMapper;
    private final StudentMapper studentMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final PackageBalanceHelper packageBalanceHelper;

    public List<StudentLessonHourLedgerResponse> listByStudent(Long studentId) {
        requireStudent(studentId);
        return ledgerMapper
                .selectList(
                        Wrappers.<StudentLessonHourLedger>lambdaQuery()
                                .eq(
                                        StudentLessonHourLedger::getTenantId,
                                        TenantContext.getTenantId())
                                .eq(StudentLessonHourLedger::getStudentId, studentId)
                                .orderByDesc(StudentLessonHourLedger::getOccurredAt)
                                .orderByDesc(StudentLessonHourLedger::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StudentLessonHourLedgerResponse adjust(Long studentId, LessonHourAdjustRequest request) {
        Student student = requireStudent(studentId);
        String eventType =
                request.getEventType() != null && !request.getEventType().isBlank()
                        ? request.getEventType().trim().toUpperCase()
                        : EVENT_ADJUST;
        if (!EVENT_ADJUST.equals(eventType) && !EVENT_VOID.equals(eventType)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "调账仅支持 ADJUST 或 VOID");
        }
        if (EVENT_VOID.equals(eventType) && request.getRelatedLedgerId() == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "VOID 须指定 relatedLedgerId");
        }
        if (EVENT_VOID.equals(eventType)) {
            return reverseAttendance(student, request);
        }
        return append(
                student,
                eventType,
                request.getMinutesDelta(),
                null,
                null,
                request.getPackageId(),
                request.getNote(),
                request.getRelatedLedgerId(),
                LocalDateTime.now());
    }

    /** 出勤确认挂钩：写入 ATTEND 消耗分钟（不强制改课时包余额，余额快照可读 remaining*约定）。 */
    @Transactional
    public void appendAttend(Student student, Lesson lesson, Long lessonStudentId) {
        if (student == null
                || lesson == null
                || lesson.getStartAt() == null
                || lesson.getEndAt() == null) {
            return;
        }
        int minutes =
                (int)
                        Math.max(
                                1,
                                Duration.between(lesson.getStartAt(), lesson.getEndAt())
                                        .toMinutes());
        Long consumedPackageId = consumeLesson(student, lesson);
        append(
                student,
                EVENT_ATTEND,
                -minutes,
                lesson.getId(),
                lessonStudentId,
                consumedPackageId,
                "出勤确认",
                null,
                lesson.getStartAt());
    }

    @Transactional
    public StudentLessonHourLedgerResponse append(
            Student student,
            String eventType,
            int minutesDelta,
            Long lessonId,
            Long lessonStudentId,
            Long packageId,
            String note,
            Long relatedLedgerId,
            LocalDateTime occurredAt) {
        // Purchased rights are measured in lessons; artwork/class duration remains in minutes.
        int remainingLessons = packageBalanceHelper.totalRemaining(student.getId());

        StudentLessonHourLedger row = new StudentLessonHourLedger();
        row.setTenantId(
                TenantContext.getTenantId() != null
                        ? TenantContext.getTenantId()
                        : student.getTenantId());
        row.setBranchId(student.getBranchId());
        row.setStudentId(student.getId());
        row.setLessonId(lessonId);
        row.setLessonStudentId(lessonStudentId);
        row.setPackageId(packageId);
        row.setEventType(eventType);
        row.setMinutesDelta(minutesDelta);
        row.setLessonUnitsDelta(
                packageId == null
                        ? 0
                        : EVENT_ATTEND.equals(eventType)
                                ? -1
                                : EVENT_VOID.equals(eventType) ? 1 : 0);
        row.setBalanceAfterMinutes(null);
        row.setRemainingLessonsAfter(remainingLessons);
        row.setOccurredAt(occurredAt != null ? occurredAt : LocalDateTime.now());
        row.setOperatorId(currentUserId());
        row.setNote(note);
        row.setRelatedLedgerId(relatedLedgerId);
        ledgerMapper.insert(row);
        return toResponse(row);
    }

    private Long consumeLesson(Student student, Lesson lesson) {
        if (Integer.valueOf(4).equals(lesson.getSource())
                || jdbc.queryForObject(
                                "SELECT COUNT(*) FROM t_lesson_student WHERE tenant_id=? AND lesson_id=? AND student_id=? AND source='TRIAL' AND status='BOOKED' AND deleted_at=0",
                                Integer.class,
                                student.getTenantId(),
                                lesson.getId(),
                                student.getId())
                        > 0) {
            return null;
        }
        Long courseId =
                lesson.getClassGroupId() == null
                        ? null
                        : jdbc.queryForObject(
                                "SELECT course_id FROM t_class_group WHERE tenant_id=? AND id=? AND deleted_at=0",
                                Long.class,
                                student.getTenantId(),
                                lesson.getClassGroupId());
        var packages =
                jdbc.queryForList(
                        """
                SELECT id FROM t_course_package WHERE tenant_id=? AND student_id=? AND branch_id=?
                 AND deleted_at=0 AND remaining_lessons>frozen_lessons AND (course_id IS NULL OR course_id=?) AND (expire_date IS NULL OR expire_date>=CURRENT_DATE)
                 ORDER BY expire_date IS NULL,expire_date,id LIMIT 1 FOR UPDATE
                """,
                        Long.class,
                        student.getTenantId(),
                        student.getId(),
                        lesson.getBranchId(),
                        courseId);
        if (packages.isEmpty()) {
            throw new BizException(ErrorCode.CONFLICT, "可用课时不足，请先补充课时");
        }
        Long id = packages.get(0);
        int changed =
                jdbc.update(
                        "UPDATE t_course_package SET remaining_lessons=remaining_lessons-1,version=version+1 WHERE tenant_id=? AND id=? AND remaining_lessons>frozen_lessons",
                        student.getTenantId(),
                        id);
        if (changed != 1) {
            throw new BizException(ErrorCode.CONFLICT, "课时余额已变化，请重试");
        }
        return id;
    }

    private StudentLessonHourLedgerResponse reverseAttendance(
            Student student, LessonHourAdjustRequest request) {
        var original =
                jdbc.queryForList(
                        "SELECT id,student_id,event_type,package_id,minutes_delta FROM t_student_lesson_hour_ledger WHERE tenant_id=? AND id=? FOR UPDATE",
                        student.getTenantId(),
                        request.getRelatedLedgerId());
        if (original.isEmpty()
                || !student.getId().toString().equals(original.get(0).get("student_id").toString())
                || !EVENT_ATTEND.equals(original.get(0).get("event_type"))) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "只能冲正该学员的出勤流水");
        }
        Integer count =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_student_lesson_hour_ledger WHERE tenant_id=? AND related_ledger_id=? AND event_type='VOID'",
                        Integer.class,
                        student.getTenantId(),
                        request.getRelatedLedgerId());
        if (count > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该流水已经冲正");
        }
        Long packageId =
                original.get(0).get("package_id") == null
                        ? null
                        : ((Number) original.get(0).get("package_id")).longValue();
        if (packageId != null) {
            int updated =
                    jdbc.update(
                            "UPDATE t_course_package SET remaining_lessons=remaining_lessons+1,version=version+1 WHERE tenant_id=? AND id=? AND student_id=? AND deleted_at=0",
                            student.getTenantId(),
                            packageId,
                            student.getId());
            if (updated != 1) {
                throw new BizException(ErrorCode.CONFLICT, "原课时包不可恢复，请人工处理");
            }
        }
        int reversal = -((Number) original.get(0).get("minutes_delta")).intValue();
        return append(
                student,
                EVENT_VOID,
                reversal,
                null,
                null,
                packageId,
                request.getNote(),
                request.getRelatedLedgerId(),
                LocalDateTime.now());
    }

    private Student requireStudent(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学员不存在");
        }
        return student;
    }

    private StudentLessonHourLedgerResponse toResponse(StudentLessonHourLedger row) {
        return StudentLessonHourLedgerResponse.builder()
                .id(row.getId())
                .studentId(row.getStudentId())
                .branchId(row.getBranchId())
                .lessonId(row.getLessonId())
                .lessonStudentId(row.getLessonStudentId())
                .packageId(row.getPackageId())
                .eventType(row.getEventType())
                .minutesDelta(row.getMinutesDelta())
                .lessonUnitsDelta(row.getLessonUnitsDelta())
                .balanceAfterMinutes(row.getBalanceAfterMinutes())
                .remainingLessonsAfter(row.getRemainingLessonsAfter())
                .occurredAt(row.getOccurredAt())
                .operatorId(row.getOperatorId())
                .note(row.getNote())
                .relatedLedgerId(row.getRelatedLedgerId())
                .createdAt(row.getCreatedAt())
                .build();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUserId();
        }
        return null;
    }
}
