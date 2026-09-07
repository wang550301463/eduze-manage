package com.eduze.manage.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckOutRequest {

    @NotNull
    private Long attendanceId;

    private Long guardianId;
    private Boolean isAbnormal;
    private String abnormalNote;
}
