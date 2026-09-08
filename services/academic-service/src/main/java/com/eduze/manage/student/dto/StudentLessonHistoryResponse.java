package com.eduze.manage.student.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentLessonHistoryResponse {
    private final Long id;
    private final Long branchId;
    private final Long studentId;
    private final String studentName;
    private final Long lessonId;
    private final Long attendanceId;
    private final Long courseId;
    private final String courseName;
    private final Long classGroupId;
    private final String classGroupName;
    private final Long teacherId;
    private final String teacherName;
    private final Long classRoomId;
    private final String classRoomName;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final Integer source;
    private final String sourceLabel;
    private final Integer attendanceStatus;
    private final Integer minutes;
    private final String snapshotJson;
    private final LocalDateTime occurredAt;
    private final LocalDateTime createdAt;
}
