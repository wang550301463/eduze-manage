package com.eduze.manage.lesson.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LessonChangeLogResponse {

    private final Long id;
    private final String changeType;
    private final String beforeJson;
    private final String afterJson;
    private final String reason;
    private final Long operatorId;
    private final LocalDateTime createdAt;
}
