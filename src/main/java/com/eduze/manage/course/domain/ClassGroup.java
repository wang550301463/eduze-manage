package com.eduze.manage.course.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_class_group")
public class ClassGroup extends BaseEntity {

    private String name;
    private Long courseId;
    private Long headTeacherId;
    /** 绑定的老师可用时段（1:1）。 */
    private Long teacherAvailabilityId;
    private Integer capacity;
    private Integer status;
    private String tagColor;
}
