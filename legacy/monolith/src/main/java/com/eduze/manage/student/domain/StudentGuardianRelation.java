package com.eduze.manage.student.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.TenantBaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_student_guardian_relation")
public class StudentGuardianRelation extends TenantBaseEntity {

    private Long studentId;
    private Long guardianId;
    private String relation;
}
