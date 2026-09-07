package com.eduze.manage.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** 创建分组时现场新建老师可用时段的载荷。 */
@Getter
@Setter
public class NestedTeacherAvailabilityRequest {

    @NotNull
    private Long teacherId;

    @NotNull
    private Long branchId;

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

    @NotNull
    private LocalDate validFrom;

    private LocalDate validTo;

    private Integer status;

    private String note;
}
