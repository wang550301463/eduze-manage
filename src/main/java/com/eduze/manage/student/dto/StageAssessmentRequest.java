package com.eduze.manage.student.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;
import lombok.Data;

@Data
public class StageAssessmentRequest {

    @NotNull
    private Long stageId;

    @NotNull
    private LocalDate assessedAt;

    private Map<String, Integer> scores;

    @Size(max = 1024)
    private String comment;
}
