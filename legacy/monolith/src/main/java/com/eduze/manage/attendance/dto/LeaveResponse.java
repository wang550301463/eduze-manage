package com.eduze.manage.attendance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaveResponse {

    private Long id;
    private Long branchId;
    private Long studentId;
    private String studentName;
    private Long lessonId;
    private LocalDate leaveStartDate;
    private LocalDate leaveEndDate;
    private String reason;
    private Integer status;
    private String statusLabel;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
