package com.eduze.manage.curriculum.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.TenantBaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_curriculum_stage")
public class CurriculumStage extends TenantBaseEntity {

    private String code;
    private String name;
    private Integer ageMin;
    private Integer ageMax;
    private Integer orderNo;
    private String lorenfieldPhase;
    private String description;
}
