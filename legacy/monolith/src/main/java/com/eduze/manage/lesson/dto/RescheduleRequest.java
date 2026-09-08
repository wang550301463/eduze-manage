package com.eduze.manage.lesson.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RescheduleRequest {

    @NotNull private LocalDateTime startAt;

    @NotNull private LocalDateTime endAt;

    private Long teacherId;
    private Long classRoomId;

    @NotBlank private String reason;
}
