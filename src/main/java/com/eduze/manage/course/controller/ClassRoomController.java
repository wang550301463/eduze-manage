package com.eduze.manage.course.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.common.web.PageResult;
import com.eduze.manage.course.dto.ClassRoomRequest;
import com.eduze.manage.course.dto.ClassRoomResponse;
import com.eduze.manage.course.service.ClassRoomService;
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
@RequestMapping("/api/class-rooms")
@RequiredArgsConstructor
public class ClassRoomController {

    private final ClassRoomService classRoomService;

    @GetMapping
    @PreAuthorize("hasAuthority('classroom:read')")
    public ApiResponse<PageResult<ClassRoomResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long branchId) {
        Page<ClassRoomResponse> result = classRoomService.list(page, size, branchId);
        return ApiResponse.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('classroom:read')")
    public ApiResponse<ClassRoomResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(classRoomService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('classroom:write')")
    public ApiResponse<ClassRoomResponse> create(@Valid @RequestBody ClassRoomRequest request) {
        return ApiResponse.ok(classRoomService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('classroom:write')")
    public ApiResponse<ClassRoomResponse> update(@PathVariable Long id, @Valid @RequestBody ClassRoomRequest request) {
        return ApiResponse.ok(classRoomService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('classroom:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        classRoomService.delete(id);
        return ApiResponse.ok(null);
    }
}
