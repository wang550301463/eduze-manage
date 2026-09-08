package com.eduze.manage.lesson.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_lesson")
public class Lesson extends BaseEntity {

    private Long classGroupId;
    private Long classRoomId;
    private Long teacherId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;
    private String note;
    private Long teacherAvailabilityId;
    private Integer source;
}
