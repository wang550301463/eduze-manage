package com.eduze.manage.teacher.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class TeacherAvailabilityRequest {

    @NotNull private Long branchId;

    @NotNull
    @Min(1)
    @Max(7)
    private Integer dayOfWeek;

    @NotNull
    @Min(0)
    @Max(1439)
    private Integer startMinute;

    @NotNull
    @Min(0)
    @Max(1439)
    private Integer endMinute;

    @NotNull
    @Min(1)
    private Integer capacity;

    private Long defaultClassRoomId;

    @NotNull private LocalDate validFrom;

    private LocalDate validTo;

    @NotNull private Integer status;

    private String note;
}
