package com.eduze.manage.student.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 学员历史课时审计流水（只追加）。 */
@Getter
@Setter
@TableName("t_student_lesson_hour_ledger")
public class StudentLessonHourLedger extends BaseEntity {

    private Long studentId;
    private Long lessonId;
    private Long lessonStudentId;
    private Long packageId;
    private String eventType;
    private Integer minutesDelta;
    private Integer lessonUnitsDelta;
    private String externalReference;
    private Integer balanceAfterMinutes;
    private Integer remainingLessonsAfter;
    private LocalDateTime occurredAt;
    private Long operatorId;
    private String note;
    private Long relatedLedgerId;
}
