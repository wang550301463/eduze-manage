package com.eduze.manage.lesson.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriptionResponse {
    private final Long id;
    private final Long studentId;
    private final String studentName;
    private final Long teacherId;
    private final String teacherName;
    private final Long teacherAvailabilityId;
    private final Long branchId;
    private final Integer dayOfWeek;
    private final Integer startMinute;
    private final Integer endMinute;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final Integer status;
    private final String source;
    private final String note;
}
