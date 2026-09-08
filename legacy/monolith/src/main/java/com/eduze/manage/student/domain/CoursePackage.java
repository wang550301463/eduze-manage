package com.eduze.manage.student.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_course_package")
public class CoursePackage extends BaseEntity {

    private Long studentId;
    private Integer totalLessons;
    private Integer remainingLessons;
    private LocalDate expireDate;
    private String note;
}
