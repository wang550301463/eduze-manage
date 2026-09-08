package com.eduze.manage.student.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_student")
public class Student extends BaseEntity {

    private String enrollNo;
    private String name;
    private Integer gender;
    private LocalDate birthday;
    private LocalDate enrollDate;
    private Integer status;
    private String allergy;
    private String healthNote;
    private String emergencyContact;
    private String emergencyPhone;
    private String avatarUrl;
    private Long mentorTeacherId;
    private Long currentStageId;
}
