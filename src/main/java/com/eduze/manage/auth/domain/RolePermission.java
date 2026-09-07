package com.eduze.manage.auth.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_role_permission")
public class RolePermission {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long roleId;
    private Long permissionId;
    private LocalDateTime createdAt;
}
