package com.eduze.manage.student.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LessonHourAdjustRequest {

    @NotNull
    private Integer minutesDelta;

    @Size(max = 512)
    private String note;

    private Long packageId;

    /** 冲正时指向被作废的流水 ID；可选。 */
    private Long relatedLedgerId;

    /** ADJUST 或 VOID，默认 ADJUST。 */
    private String eventType;
}
