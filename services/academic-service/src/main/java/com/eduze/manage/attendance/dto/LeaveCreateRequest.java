package com.eduze.manage.attendance.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveCreateRequest {

    @NotNull private Long studentId;

    private Long lessonId;

    @NotNull private LocalDate leaveStartDate;

    @NotNull private LocalDate leaveEndDate;

    private String reason;
}
