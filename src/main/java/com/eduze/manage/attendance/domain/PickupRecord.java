package com.eduze.manage.attendance.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_pickup_record")
public class PickupRecord extends BaseEntity {

    private Long attendanceId;
    private String eventType;
    private Long guardianId;
    private Integer isAbnormal;
    private String abnormalNote;
    private LocalDateTime eventTime;
}
