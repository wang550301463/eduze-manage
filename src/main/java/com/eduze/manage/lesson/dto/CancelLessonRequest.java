package com.eduze.manage.lesson.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelLessonRequest {

    @NotBlank
    private String reason;
}
