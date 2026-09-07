package com.eduze.manage.lesson.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LessonStudentRequest {

    @NotNull
    private Long studentId;

    /** SUBSCRIPTION/MANUAL/TRIAL/MAKEUP，默认 MANUAL */
    @Size(max = 16)
    private String source;

    @Size(max = 256)
    private String note;
}
