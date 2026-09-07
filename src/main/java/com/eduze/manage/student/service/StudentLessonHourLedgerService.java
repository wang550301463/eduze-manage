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

/**
 * 学员课时审计流水：只 INSERT，纠错走 VOID/ADJUST 新行。
 */
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
    private final PackageBalanceHelper packageBalanceHelper;

    public List<StudentLessonHourLedgerResponse> listByStudent(Long studentId) {
        requireStudent(studentId);
        return ledgerMapper
                .selectList(Wrappers.<StudentLessonHourLedger>lambdaQuery()
                        .eq(StudentLessonHourLedger::getTenantId, TenantContext.getTenantId())
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
        String eventType = request.getEventType() != null && !request.getEventType().isBlank()
                ? request.getEventType().trim().toUpperCase()
                : EVENT_ADJUST;
        if (!EVENT_ADJUST.equals(eventType) && !EVENT_VOID.equals(eventType)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "调账仅支持 ADJUST 或 VOID");
        }
        if (EVENT_VOID.equals(eventType) && request.getRelatedLedgerId() == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "VOID 须指定 relatedLedgerId");
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

    /**
     * 出勤确认挂钩：写入 ATTEND 消耗分钟（不强制改课时包余额，余额快照可读 remaining*约定）。
     */
    @Transactional
    public void appendAttend(Student student, Lesson lesson, Long lessonStudentId) {
        if (student == null || lesson == null || lesson.getStartAt() == null || lesson.getEndAt() == null) {
            return;
        }
        int minutes = (int) Math.max(1, Duration.between(lesson.getStartAt(), lesson.getEndAt()).toMinutes());
        append(
                student,
                EVENT_ATTEND,
                -minutes,
                lesson.getId(),
                lessonStudentId,
                null,
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
        // 余额快照：课时包 remaining 按「节」计，流水按分钟；快照用 remaining * 60 近似可读
        int remainingLessons = packageBalanceHelper.totalRemaining(student.getId());
        int balanceAfter = remainingLessons * 60 + minutesDelta;

        StudentLessonHourLedger row = new StudentLessonHourLedger();
        row.setTenantId(TenantContext.getTenantId() != null ? TenantContext.getTenantId() : student.getTenantId());
        row.setBranchId(student.getBranchId());
        row.setStudentId(student.getId());
        row.setLessonId(lessonId);
        row.setLessonStudentId(lessonStudentId);
        row.setPackageId(packageId);
        row.setEventType(eventType);
        row.setMinutesDelta(minutesDelta);
        row.setBalanceAfterMinutes(balanceAfter);
        row.setOccurredAt(occurredAt != null ? occurredAt : LocalDateTime.now());
        row.setOperatorId(currentUserId());
        row.setNote(note);
        row.setRelatedLedgerId(relatedLedgerId);
        ledgerMapper.insert(row);
        return toResponse(row);
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
                .balanceAfterMinutes(row.getBalanceAfterMinutes())
                .occurredAt(row.getOccurredAt())
                .operatorId(row.getOperatorId())
                .note(row.getNote())
                .relatedLedgerId(row.getRelatedLedgerId())
                .createdAt(row.getCreatedAt())
                .build();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUserId();
        }
        return null;
    }
}
