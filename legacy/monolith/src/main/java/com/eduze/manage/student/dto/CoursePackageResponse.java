package com.eduze.manage.student.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CoursePackageResponse {

    private final Long id;
    private final Long studentId;
    private final Long branchId;
    private final Integer totalLessons;
    private final Integer remainingLessons;
    private final LocalDate expireDate;
    private final String note;
    private final Boolean alertLow;
}
