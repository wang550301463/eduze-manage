package com.eduze.manage.course.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassMemberResponse {

    private final Long studentId;
    private final String studentName;
    private final String enrollNo;
    private final LocalDateTime joinedAt;
    private final LocalDateTime leftAt;
}
