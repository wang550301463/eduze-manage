package com.eduze.manage.attendance.controller;

import com.eduze.manage.attendance.dto.AttendanceResponse;
import com.eduze.manage.attendance.dto.CheckInRequest;
import com.eduze.manage.attendance.dto.CheckOutRequest;
import com.eduze.manage.attendance.dto.TodayRosterResponse;
import com.eduze.manage.attendance.dto.UpdateAttendanceRequest;
import com.eduze.manage.attendance.service.AttendanceService;
import com.eduze.manage.audit.AuditAction;
import com.eduze.manage.common.web.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final com.eduze.manage.tenant.BranchAccessGuard branchAccessGuard;

    @GetMapping("/today")
    @PreAuthorize("hasAuthority('attendance:read')")
    public ApiResponse<TodayRosterResponse> today(
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "morning") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        branchAccessGuard.requireBranchAccess(branchId);
        return ApiResponse.ok(attendanceService.todayRoster(branchId, period, date));
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyAuthority('attendance:write', 'attendance:scan')")
    @AuditAction(action = "ATTENDANCE_CHECK_IN", entityType = "attendance", entityIdSpEL = "#request.studentId")
    public ApiResponse<AttendanceResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ApiResponse.ok(attendanceService.checkIn(request));
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAuthority('attendance:write')")
    public ApiResponse<AttendanceResponse> checkOut(@Valid @RequestBody CheckOutRequest request) {
        return ApiResponse.ok(attendanceService.checkOut(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('attendance:write')")
    public ApiResponse<AttendanceResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateAttendanceRequest request) {
        return ApiResponse.ok(attendanceService.updateStatus(id, request));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAuthority('attendance:read')")
    public ApiResponse<List<AttendanceResponse>> studentHistory(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(attendanceService.listByStudent(studentId, from, to));
    }
}
