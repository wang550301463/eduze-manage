package com.eduze.manage.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassRoomRequest {

    @NotNull
    private Long branchId;

    @NotBlank
    @Size(max = 128)
    private String name;

    private Integer capacity;
    private String note;
}
