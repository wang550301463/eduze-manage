package com.eduze.manage.lesson.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LessonResponse {

    private final Long id;
    private final Long tenantId;
    private final Long branchId;
    private final Long classGroupId;
    private final String classGroupName;
    private final Long courseId;
    private final String courseName;
    private final Long classRoomId;
    private final String classRoomName;
    private final Long teacherId;
    private final String teacherName;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final String status;
    private final String note;
}
