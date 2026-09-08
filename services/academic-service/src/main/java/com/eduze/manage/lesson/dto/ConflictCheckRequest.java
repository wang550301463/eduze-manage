package com.eduze.manage.lesson.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConflictCheckRequest {

    private Long lessonId;

    @NotNull private Long branchId;

    @NotNull private Long classGroupId;

    private Long classRoomId;
    private Long teacherId;

    @NotNull private LocalDateTime startAt;

    @NotNull private LocalDateTime endAt;
}
