package com.eduze.manage.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseRequest {

    @NotBlank
    @Size(max = 128)
    private String name;

    private Integer ageMin;
    private Integer ageMax;
    private Integer lessonMinutes;
    private String coverUrl;
    private String description;
}
