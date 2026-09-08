package com.eduze.manage.audit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_audit_log")
public class AuditLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long branchId;

    private Long userId;

    private String username;

    private String action;

    private String entityType;

    private String entityId;

    private String ip;

    private String userAgent;

    private String requestPath;

    private String status;

    private String errorMsg;

    private String extraJson;

    private LocalDateTime createdAt;
}
