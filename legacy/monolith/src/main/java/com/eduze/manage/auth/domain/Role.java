package com.eduze.manage.auth.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.TenantBaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_role")
public class Role extends TenantBaseEntity {

    private String code;
    private String name;
    private Integer isBuiltin;
}
