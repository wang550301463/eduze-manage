package com.eduze.manage.teacher.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_teacher_availability")
public class TeacherAvailability extends BaseEntity {

    private Long teacherId;
    private Integer dayOfWeek;
    private Integer startMinute;
    private Integer endMinute;
    private Integer capacity;
    private Long defaultClassRoomId;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer status;
    private String note;
}
