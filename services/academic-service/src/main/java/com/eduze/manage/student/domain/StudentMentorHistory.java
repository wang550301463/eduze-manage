package com.eduze.manage.student.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_student_mentor_history")
public class StudentMentorHistory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long branchId;
    private Long studentId;
    private Long fromTeacherId;
    private Long toTeacherId;
    private String reason;
    private LocalDateTime changedAt;
    private Long operatorId;
    private LocalDateTime createdAt;
}
