package com.eduze.manage.student.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StageAssessmentResponse {
    private final Long id;
    private final Long studentId;
    private final Long stageId;
    private final String stageCode;
    private final String stageName;
    private final LocalDate assessedAt;
    private final Long assessedBy;
    private final Map<String, Integer> scores;
    private final String comment;
    private final LocalDateTime createdAt;
}
