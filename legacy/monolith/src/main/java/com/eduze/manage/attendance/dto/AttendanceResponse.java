package com.eduze.manage.attendance.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttendanceResponse {

    private Long id;
    private Long lessonId;
    private Long studentId;
    private String studentName;
    private String classGroupName;
    private Integer status;
    private String statusLabel;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private String checkInMethod;
    private String note;
    private LocalDateTime lessonStartAt;
    private LocalDateTime lessonEndAt;
}
