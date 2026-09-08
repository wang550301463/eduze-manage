package com.eduze.manage.course.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.TenantBaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_course")
public class Course extends TenantBaseEntity {

    private String name;
    private Integer ageMin;
    private Integer ageMax;
    private Integer lessonMinutes;
    private String coverUrl;
    private String description;
}
