package com.eduze.manage.attendance.controller;

import com.eduze.manage.attendance.dto.LeaveCreateRequest;
import com.eduze.manage.attendance.dto.LeaveResponse;
import com.eduze.manage.attendance.service.LeaveService;
import com.eduze.manage.audit.AuditAction;
import com.eduze.manage.common.web.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    @PreAuthorize("hasAuthority('leave:write')")
    @AuditAction(action = "LEAVE_CREATE", entityType = "leave", entityIdSpEL = "#request.studentId")
    public ApiResponse<LeaveResponse> create(@Valid @RequestBody LeaveCreateRequest request) {
        return ApiResponse.ok(leaveService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('leave:read')")
    public ApiResponse<List<LeaveResponse>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to) {
        return ApiResponse.ok(leaveService.list(status, studentId, from, to));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('leave:approve')")
    @AuditAction(action = "LEAVE_APPROVE", entityType = "leave", entityIdSpEL = "#id")
    public ApiResponse<LeaveResponse> approve(@PathVariable Long id) {
        return ApiResponse.ok(leaveService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('leave:approve')")
    public ApiResponse<LeaveResponse> reject(@PathVariable Long id) {
        return ApiResponse.ok(leaveService.reject(id));
    }
}
