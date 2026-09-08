package com.eduze.manage.student.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentLessonHourLedgerResponse {
    private final Long id;
    private final Long studentId;
    private final Long branchId;
    private final Long lessonId;
    private final Long lessonStudentId;
    private final Long packageId;
    private final String eventType;
    private final Integer minutesDelta;
    private final Integer lessonUnitsDelta;
    private final Integer balanceAfterMinutes;
    private final Integer remainingLessonsAfter;
    private final LocalDateTime occurredAt;
    private final Long operatorId;
    private final String note;
    private final Long relatedLedgerId;
    private final LocalDateTime createdAt;
}
