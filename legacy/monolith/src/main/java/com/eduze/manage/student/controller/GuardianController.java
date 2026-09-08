package com.eduze.manage.student.controller;

import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.student.dto.GuardianQrResponse;
import com.eduze.manage.student.dto.GuardianRequest;
import com.eduze.manage.student.dto.GuardianResponse;
import com.eduze.manage.student.service.GuardianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService guardianService;

    @PostMapping
    @PreAuthorize("hasAuthority('guardian:write')")
    public ApiResponse<GuardianResponse> create(@Valid @RequestBody GuardianRequest request) {
        return ApiResponse.ok(guardianService.create(request));
    }

    @PostMapping("/{id}/qr")
    @PreAuthorize("hasAuthority('guardian:write')")
    public ApiResponse<GuardianQrResponse> generateQr(@PathVariable Long id) {
        return ApiResponse.ok(guardianService.generateQr(id));
    }
}
