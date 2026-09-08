package com.eduze.manage.attendance.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_leave_request")
public class LeaveRequest extends BaseEntity {

    private Long studentId;
    private Long lessonId;
    private LocalDate leaveStartDate;
    private LocalDate leaveEndDate;
    private String reason;
    private Integer status;
    private Long approvedBy;
    private LocalDateTime approvedAt;
}
