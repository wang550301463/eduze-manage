package com.eduze.manage.lesson.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConflictReport {

    private final boolean hasConflict;
    private final LessonResponse teacher;
    private final LessonResponse classRoom;
    private final LessonResponse classGroup;
}
