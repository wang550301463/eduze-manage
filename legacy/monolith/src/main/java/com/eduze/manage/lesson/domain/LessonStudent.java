package com.eduze.manage.lesson.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_lesson_student")
public class LessonStudent extends BaseEntity {

    private Long lessonId;
    private Long studentId;
    private Long subscriptionId;
    private String source;
    private String status;
    private String note;
    private LocalDateTime removedAt;
    private Long removedBy;
}
