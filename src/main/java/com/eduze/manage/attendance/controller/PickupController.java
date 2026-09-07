package com.eduze.manage.attendance.controller;

import com.eduze.manage.attendance.dto.PickupRecordRequest;
import com.eduze.manage.attendance.dto.PickupRecordResponse;
import com.eduze.manage.attendance.service.PickupService;
import com.eduze.manage.common.web.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pickup-records")
@RequiredArgsConstructor
public class PickupController {

    private final PickupService pickupService;

    @GetMapping
    @PreAuthorize("hasAuthority('pickup:read')")
    public ApiResponse<List<PickupRecordResponse>> list(
            @RequestParam(required = false) Long lessonId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Integer isAbnormal) {
        return ApiResponse.ok(pickupService.list(lessonId, studentId, isAbnormal));
    }

    @GetMapping("/abnormal")
    @PreAuthorize("hasAuthority('pickup:read')")
    public ApiResponse<List<PickupRecordResponse>> abnormal(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime to) {
        return ApiResponse.ok(pickupService.listAbnormal(from, to));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('pickup:write')")
    public ApiResponse<PickupRecordResponse> create(@Valid @RequestBody PickupRecordRequest request) {
        return ApiResponse.ok(pickupService.create(request));
    }
}
