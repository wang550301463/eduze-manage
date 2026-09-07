package com.eduze.manage.student.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MentorHistoryResponse {
    private final Long id;
    private final Long fromTeacherId;
    private final String fromTeacherName;
    private final Long toTeacherId;
    private final String toTeacherName;
    private final String reason;
    private final LocalDateTime changedAt;
    private final Long operatorId;
}
