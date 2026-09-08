package com.eduze.manage.attendance.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BranchAttendanceStat {

    private Long branchId;
    private int total;
    private int present;
    private int absent;
    private int leave;
    private BigDecimal rate;
    private List<ClassGroupAttendanceStat> classGroups;
}
