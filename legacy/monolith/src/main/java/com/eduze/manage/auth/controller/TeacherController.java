package com.eduze.manage.auth.controller;

import com.eduze.manage.auth.dto.TeacherSummaryResponse;
import com.eduze.manage.auth.service.TeacherQueryService;
import com.eduze.manage.common.web.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherQueryService teacherQueryService;

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ApiResponse<List<TeacherSummaryResponse>> list(
            @RequestParam(required = false) Long branchId) {
        return ApiResponse.ok(teacherQueryService.list(branchId));
    }
}
