package com.eduze.manage.attendance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.attendance.domain.AttendanceStatus;
import com.eduze.manage.attendance.domain.LeaveRequest;
import com.eduze.manage.attendance.domain.LeaveStatus;
import com.eduze.manage.attendance.mapper.LeaveRequestMapper;
import com.eduze.manage.student.domain.Guardian;
import com.eduze.manage.student.domain.StudentGuardianRelation;
import com.eduze.manage.student.mapper.GuardianMapper;
import com.eduze.manage.student.mapper.StudentGuardianRelationMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceSupport {

    private final LeaveRequestMapper leaveRequestMapper;
    private final StudentGuardianRelationMapper relationMapper;
    private final GuardianMapper guardianMapper;

    public boolean hasApprovedLeave(Long studentId, LocalDate date) {
        Long count =
                leaveRequestMapper.selectCount(
                        Wrappers.<LeaveRequest>lambdaQuery()
                                .eq(LeaveRequest::getTenantId, TenantContext.getTenantId())
                                .eq(LeaveRequest::getStudentId, studentId)
                                .eq(LeaveRequest::getStatus, LeaveStatus.APPROVED.getCode())
                                .le(LeaveRequest::getLeaveStartDate, date)
                                .ge(LeaveRequest::getLeaveEndDate, date));
        return count != null && count > 0;
    }

    public boolean isAuthorizedPickup(Long studentId, Long guardianId) {
        if (guardianId == null) {
            return false;
        }
        Long linked =
                relationMapper.selectCount(
                        Wrappers.<StudentGuardianRelation>lambdaQuery()
                                .eq(
                                        StudentGuardianRelation::getTenantId,
                                        TenantContext.getTenantId())
                                .eq(StudentGuardianRelation::getStudentId, studentId)
                                .eq(StudentGuardianRelation::getGuardianId, guardianId));
        if (linked == null || linked == 0) {
            return false;
        }
        Guardian guardian = guardianMapper.selectById(guardianId);
        return guardian != null && guardian.getCanPickup() != null && guardian.getCanPickup() == 1;
    }

    public static String statusLabel(Integer status) {
        if (status == null) {
            return "未到";
        }
        return switch (status) {
            case 2 -> AttendanceStatus.CHECKED_IN.getLabel();
            case 3 -> AttendanceStatus.CHECKED_OUT.getLabel();
            case 4 -> AttendanceStatus.ABSENT.getLabel();
            case 5 -> AttendanceStatus.LEAVE.getLabel();
            default -> "未到";
        };
    }

    public static LocalTime periodStart(String period) {
        return switch (period == null ? "morning" : period) {
            case "afternoon" -> LocalTime.NOON;
            case "evening" -> LocalTime.of(18, 0);
            default -> LocalTime.MIDNIGHT;
        };
    }

    public static LocalTime periodEnd(String period) {
        return switch (period == null ? "morning" : period) {
            case "morning" -> LocalTime.NOON;
            case "afternoon" -> LocalTime.of(18, 0);
            default -> LocalTime.MAX;
        };
    }
}
