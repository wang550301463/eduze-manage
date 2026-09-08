package com.eduze.manage.student.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.TenantBaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_guardian")
public class Guardian extends TenantBaseEntity {

    private String name;
    private String phone;
    private Integer isMainContact;
    private Integer canPickup;
    private String qrCode;
}
