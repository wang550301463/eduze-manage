package com.eduze.manage.student.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CoursePackageRequest {

    @NotNull
    @Min(1)
    private Integer totalLessons;

    @NotNull
    @Min(0)
    private Integer remainingLessons;

    private LocalDate expireDate;
    private Long courseId;

    @Size(max = 256)
    private String note;
}
