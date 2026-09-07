package com.eduze.manage.teacher.controller;

import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.teacher.dto.TeacherAvailabilityRequest;
import com.eduze.manage.teacher.dto.TeacherAvailabilityResponse;
import com.eduze.manage.teacher.service.TeacherAvailabilityService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TeacherAvailabilityController {

    private final TeacherAvailabilityService service;

    @GetMapping("/api/teachers/{teacherId}/availabilities")
    @PreAuthorize("hasAuthority('teacher:availability:read')")
    public ApiResponse<List<TeacherAvailabilityResponse>> list(@PathVariable Long teacherId) {
        return ApiResponse.ok(service.list(teacherId));
    }

    @PostMapping("/api/teachers/{teacherId}/availabilities")
    @PreAuthorize("hasAuthority('teacher:availability:write')")
    public ApiResponse<TeacherAvailabilityResponse> create(
            @PathVariable Long teacherId, @Valid @RequestBody TeacherAvailabilityRequest req) {
        return ApiResponse.ok(service.create(teacherId, req));
    }

    @PutMapping("/api/teacher-availabilities/{id}")
    @PreAuthorize("hasAuthority('teacher:availability:write')")
    public ApiResponse<TeacherAvailabilityResponse> update(
            @PathVariable Long id, @Valid @RequestBody TeacherAvailabilityRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/api/teacher-availabilities/{id}")
    @PreAuthorize("hasAuthority('teacher:availability:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/teacher-availabilities/conflicts")
    @PreAuthorize("hasAuthority('teacher:availability:read')")
    public ApiResponse<Map<String, Boolean>> checkConflict(
            @RequestParam Long teacherId,
            @RequestParam Integer dayOfWeek,
            @RequestParam Integer start,
            @RequestParam Integer end) {
        boolean conflict = service.hasOverlap(teacherId, dayOfWeek, start, end);
        return ApiResponse.ok(Map.of("conflict", conflict));
    }
}
