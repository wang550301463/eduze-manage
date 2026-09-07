package com.eduze.manage.lesson.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class SubscriptionRequest {

    @NotNull
    private Long studentId;

    @NotNull
    private Long teacherAvailabilityId;

    @NotNull
    private LocalDate validFrom;

    private LocalDate validTo;

    private Integer status;

    @Size(max = 16)
    private String source;

    @Size(max = 256)
    private String note;
}
