package com.eduze.manage.lesson.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleResponse {

    private final LocalDate weekStart;
    private final LocalDate weekEnd;
    private final List<DaySchedule> days;

    @Getter
    @Builder
    public static class DaySchedule {
        private final LocalDate date;
        private final List<ScheduleLessonItem> lessons;
    }

    @Getter
    @Builder
    public static class ScheduleLessonItem {
        private final Long id;
        private final Long classGroupId;
        private final String classGroupName;
        private final Long courseId;
        private final String courseName;
        private final Long teacherId;
        private final String teacherShortName;
        private final Long classRoomId;
        private final String classRoomShortName;
        private final String startAt;
        private final String endAt;
        private final String status;
        private final String color;
    }
}
