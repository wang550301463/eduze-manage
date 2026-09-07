package com.eduze.manage.teacher.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherAvailabilityResponse {
    private final Long id;
    private final Long teacherId;
    private final Long branchId;
    private final Integer dayOfWeek;
    private final Integer startMinute;
    private final Integer endMinute;
    private final Integer capacity;
    private final Long defaultClassRoomId;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final Integer status;
    private final String note;
}
