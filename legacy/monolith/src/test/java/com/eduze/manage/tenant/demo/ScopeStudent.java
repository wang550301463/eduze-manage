package com.eduze.manage.tenant.demo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_student")
public class ScopeStudent extends BaseEntity {

    private String name;
}
