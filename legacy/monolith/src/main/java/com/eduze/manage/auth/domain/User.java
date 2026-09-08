package com.eduze.manage.auth.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.TenantBaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_user")
public class User extends TenantBaseEntity {

    private Long branchId;
    private String username;
    private String passwordHash;
    private String name;
    private String phone;
    private String email;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private Integer tokenVersion;
}
