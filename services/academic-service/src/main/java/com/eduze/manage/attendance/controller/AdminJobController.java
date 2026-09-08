package com.eduze.manage.attendance.controller;

import com.eduze.manage.attendance.service.AbsenceJob;
import com.eduze.manage.common.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/jobs")
@RequiredArgsConstructor
public class AdminJobController {

    private final AbsenceJob absenceJob;

    @PostMapping("/absence:run")
    @PreAuthorize("hasAuthority('audit:read')")
    public ApiResponse<Void> runAbsenceJob() {
        absenceJob.runWithLock();
        return ApiResponse.ok(null);
    }
}
