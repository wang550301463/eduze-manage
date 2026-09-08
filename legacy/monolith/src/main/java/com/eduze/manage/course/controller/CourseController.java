package com.eduze.manage.course.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.common.web.PageResult;
import com.eduze.manage.course.dto.CourseRequest;
import com.eduze.manage.course.dto.CourseResponse;
import com.eduze.manage.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @PreAuthorize("hasAuthority('course:read')")
    public ApiResponse<PageResult<CourseResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        Page<CourseResponse> result = courseService.list(page, size, keyword);
        return ApiResponse.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('course:read')")
    public ApiResponse<CourseResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(courseService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('course:write')")
    public ApiResponse<CourseResponse> create(@Valid @RequestBody CourseRequest request) {
        return ApiResponse.ok(courseService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('course:write')")
    public ApiResponse<CourseResponse> update(
            @PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return ApiResponse.ok(courseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('course:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ApiResponse.ok(null);
    }
}
