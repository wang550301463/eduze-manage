package com.eduze.manage.lesson.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.TenantBaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_lesson_change_log")
public class LessonChangeLog extends TenantBaseEntity {

    private Long lessonId;
    private String changeType;
    private String beforeJson;
    private String afterJson;
    private String reason;
    private Long operatorId;
}
