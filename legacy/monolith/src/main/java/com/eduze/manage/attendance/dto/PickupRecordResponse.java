package com.eduze.manage.attendance.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PickupRecordResponse {

    private Long id;
    private Long attendanceId;
    private Long lessonId;
    private Long studentId;
    private String studentName;
    private String eventType;
    private Long guardianId;
    private String guardianName;
    private Integer isAbnormal;
    private String abnormalNote;
    private LocalDateTime eventTime;
}
