package com.eduze.manage.attendance.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentAttendanceStat {

    private int total;
    private int present;
    private int absent;
    private int leave;
    private BigDecimal rate;
}
