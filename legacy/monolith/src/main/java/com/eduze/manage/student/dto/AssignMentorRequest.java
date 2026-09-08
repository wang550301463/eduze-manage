package com.eduze.manage.student.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AssignMentorRequest {

    @NotNull private Long toTeacherId;

    @Size(max = 256)
    private String reason;

    /** 是否保留学员既有订阅，默认 false（同时关闭旧订阅） */
    private Boolean keepSubscriptions;
}
