package com.eduze.manage.lesson.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherScheduleResponse {

    private final LocalDate weekStart;
    private final List<TeacherColumn> columns;

    @Getter
    @Builder
    public static class TeacherColumn {
        private final Long teacherId;
        private final String teacherName;
        private final Long branchId;
        private final List<LessonCell> lessons;
    }

    @Getter
    @Builder
    public static class LessonCell {
        private final Long lessonId;
        private final Long classRoomId;
        private final String classRoomName;
        private final LocalDateTime startAt;
        private final LocalDateTime endAt;
        private final Integer dayOfWeek;
        private final Integer startMinute;
        private final Integer endMinute;
        private final Integer capacity;
        private final Integer studentCount;
        private final Integer source;
        private final Long teacherAvailabilityId;
        private final String status;
    }
}
