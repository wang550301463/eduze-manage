package com.eduze.manage.attendance.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_attendance")
public class Attendance extends BaseEntity {

    private Long lessonId;
    private Long studentId;
    private Integer status;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private String checkInMethod;
    private String note;
}
