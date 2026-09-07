package com.eduze.manage.course.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.common.web.PageResult;
import com.eduze.manage.course.dto.ClassGroupRequest;
import com.eduze.manage.course.dto.ClassGroupResponse;
import com.eduze.manage.course.service.ClassGroupService;
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
@RequestMapping("/api/class-groups")
@RequiredArgsConstructor
public class ClassGroupController {

    private final ClassGroupService classGroupService;

    @GetMapping
    @PreAuthorize("hasAuthority('classgroup:read')")
    public ApiResponse<PageResult<ClassGroupResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long courseId) {
        Page<ClassGroupResponse> result = classGroupService.list(page, size, branchId, courseId);
        return ApiResponse.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('classgroup:read')")
    public ApiResponse<ClassGroupResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(classGroupService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('classgroup:write')")
    public ApiResponse<ClassGroupResponse> create(@Valid @RequestBody ClassGroupRequest request) {
        return ApiResponse.ok(classGroupService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('classgroup:write')")
    public ApiResponse<ClassGroupResponse> update(@PathVariable Long id, @Valid @RequestBody ClassGroupRequest request) {
        return ApiResponse.ok(classGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('classgroup:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        classGroupService.delete(id);
        return ApiResponse.ok(null);
    }
}
