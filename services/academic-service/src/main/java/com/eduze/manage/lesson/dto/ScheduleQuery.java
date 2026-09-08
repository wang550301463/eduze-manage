package com.eduze.manage.lesson.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleQuery {

    private Long branchId;
    private LocalDate weekStart;
}
