package com.eduze.manage.student.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentStatusRequest {

    @NotNull
    @Min(1)
    @Max(3)
    private Integer status;
}
