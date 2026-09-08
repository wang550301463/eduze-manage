package com.eduze.manage.course.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_class_room")
public class ClassRoom extends BaseEntity {

    private String name;
    private Integer capacity;
    private String note;
}
