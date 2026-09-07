package com.eduze.manage.course.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_student_class_group")
public class StudentClassGroup extends BaseEntity {

    private Long studentId;
    private Long classGroupId;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;

    @Override
    public Long getBranchId() {
        return null;
    }

    @Override
    public void setBranchId(Long branchId) {
        // membership is tenant-scoped
    }

    @TableField(exist = false)
    private Long branchId;
}
