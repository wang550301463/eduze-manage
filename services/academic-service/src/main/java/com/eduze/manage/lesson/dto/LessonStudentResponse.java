package com.eduze.manage.lesson.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LessonStudentResponse {
    private final Long id;
    private final Long lessonId;
    private final Long studentId;
    private final String studentName;
    private final Long subscriptionId;
    private final String source;
    private final String status;
    private final String note;
}
