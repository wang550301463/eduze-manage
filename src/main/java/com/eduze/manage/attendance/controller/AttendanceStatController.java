package com.eduze.manage.attendance.controller;

import com.eduze.manage.attendance.dto.BranchAttendanceStat;
import com.eduze.manage.attendance.dto.ClassGroupAttendanceStat;
import com.eduze.manage.attendance.dto.StudentAttendanceStat;
import com.eduze.manage.attendance.service.AttendanceStatService;
import com.eduze.manage.common.web.ApiResponse;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats/attendance")
@RequiredArgsConstructor
public class AttendanceStatController {

    private final AttendanceStatService attendanceStatService;
    private final com.eduze.manage.tenant.BranchAccessGuard branchAccessGuard;

    @GetMapping("/student/{id}")
    @PreAuthorize("hasAuthority('stat:read')")
    public ApiResponse<StudentAttendanceStat> student(
            @PathVariable("id") Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(attendanceStatService.studentStat(studentId, from, to));
    }

    @GetMapping("/class-group/{id}")
    @PreAuthorize("hasAuthority('stat:read')")
    public ApiResponse<ClassGroupAttendanceStat> classGroup(
            @PathVariable("id") Long classGroupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(attendanceStatService.classGroupStat(classGroupId, from, to));
    }

    @GetMapping("/branch/{id}")
    @PreAuthorize("hasAuthority('stat:read')")
    public ApiResponse<BranchAttendanceStat> branch(
            @PathVariable("id") Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        branchAccessGuard.requireBranchAccess(branchId);
        return ApiResponse.ok(attendanceStatService.branchStat(branchId, from, to));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('stat:read')")
    public ApiResponse<Map<String, Object>> dashboard(@RequestParam Long branchId) {
        branchAccessGuard.requireBranchAccess(branchId);
        return ApiResponse.ok(attendanceStatService.dashboardKpis(branchId));
    }
}
