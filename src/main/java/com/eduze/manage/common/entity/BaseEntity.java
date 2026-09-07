package com.eduze.manage.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseEntity extends TenantBaseEntity {

    @TableField(fill = FieldFill.INSERT)
    private Long branchId;
}
