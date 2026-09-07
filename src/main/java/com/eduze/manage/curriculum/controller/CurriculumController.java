package com.eduze.manage.curriculum.controller;

import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.curriculum.dto.DimensionResponse;
import com.eduze.manage.curriculum.dto.StageResponse;
import com.eduze.manage.curriculum.service.CurriculumService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/curriculum")
@RequiredArgsConstructor
public class CurriculumController {

    private final CurriculumService service;

    @GetMapping("/stages")
    @PreAuthorize("hasAuthority('course:read')")
    public ApiResponse<List<StageResponse>> stages() {
        return ApiResponse.ok(service.listStages());
    }

    @GetMapping("/dimensions")
    @PreAuthorize("hasAuthority('course:read')")
    public ApiResponse<List<DimensionResponse>> dimensions(@RequestParam(required = false) String kind) {
        return ApiResponse.ok(service.listDimensions(kind));
    }
}
