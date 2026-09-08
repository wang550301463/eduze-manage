package com.eduze.manage.lesson.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LessonRequest {

    @NotNull private Long branchId;

    /** 特殊课（补课/考级/比赛）可不挂分组。 */
    private Long classGroupId;

    private Long classRoomId;

    private Long teacherId;

    @NotNull private LocalDateTime startAt;

    @NotNull private LocalDateTime endAt;

    private String note;

    /** 课次来源：1模板 2手动 3补课 4试听 5考级 6比赛。 默认 2；特殊课传 3/5/6。 */
    private Integer source;
}
