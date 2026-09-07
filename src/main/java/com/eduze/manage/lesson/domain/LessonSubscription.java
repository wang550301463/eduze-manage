package com.eduze.manage.lesson.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_lesson_subscription")
public class LessonSubscription extends BaseEntity {

    private Long studentId;
    private Long teacherId;
    private Long teacherAvailabilityId;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer status;
    private String source;
    private String note;
}
