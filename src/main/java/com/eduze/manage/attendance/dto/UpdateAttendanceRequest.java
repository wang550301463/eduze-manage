package com.eduze.manage.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAttendanceRequest {

    @NotNull
    private Integer status;

    private String note;
}
