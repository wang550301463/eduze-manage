package com.eduze.manage.curriculum.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.TenantBaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_curriculum_dimension")
public class CurriculumDimension extends TenantBaseEntity {

    private String kind;
    private String code;
    private String name;
    private String description;
    private Integer orderNo;
}
