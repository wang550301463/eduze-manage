package com.eduze.manage.student.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_student_stage_assessment")
public class StudentStageAssessment extends BaseEntity {

    private Long studentId;
    private Long stageId;
    private LocalDate assessedAt;
    private Long assessedBy;
    private String scoresJson;
    private String comment;
}
