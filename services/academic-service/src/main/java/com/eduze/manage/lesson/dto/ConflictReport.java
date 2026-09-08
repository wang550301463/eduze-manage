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

    /** 特殊课与已绑分组正常时段窗口冲突时非空（占位响应）。 */
    private final LessonResponse boundAvailability;
}
