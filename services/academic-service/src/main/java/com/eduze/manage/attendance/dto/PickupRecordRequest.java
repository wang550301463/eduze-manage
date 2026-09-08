package com.eduze.manage.attendance.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PickupRecordRequest {

    @NotNull private Long attendanceId;

    @NotNull private String eventType;

    private Long guardianId;
    private Boolean isAbnormal;
    private String abnormalNote;
    private LocalDateTime eventTime;
}
