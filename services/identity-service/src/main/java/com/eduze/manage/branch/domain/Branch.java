package com.eduze.manage.branch.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.TenantBaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_branch")
public class Branch extends TenantBaseEntity {

    private String name;
    private String code;
    private String address;
    private String phone;
    private Integer status;
}
