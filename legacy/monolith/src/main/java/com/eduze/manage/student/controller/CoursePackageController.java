package com.eduze.manage.student.controller;

import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.student.dto.CoursePackageRequest;
import com.eduze.manage.student.dto.CoursePackageResponse;
import com.eduze.manage.student.service.CoursePackageService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CoursePackageController {

    private final CoursePackageService coursePackageService;

    @GetMapping("/api/students/{studentId}/packages")
    @PreAuthorize("hasAuthority('coursepkg:read')")
    public ApiResponse<List<CoursePackageResponse>> listByStudent(@PathVariable Long studentId) {
        return ApiResponse.ok(coursePackageService.listByStudent(studentId));
    }

    @PostMapping("/api/students/{studentId}/packages")
    @PreAuthorize("hasAuthority('coursepkg:write')")
    public ApiResponse<CoursePackageResponse> create(
            @PathVariable Long studentId, @Valid @RequestBody CoursePackageRequest request) {
        return ApiResponse.ok(coursePackageService.create(studentId, request));
    }

    @PutMapping("/api/packages/{id}")
    @PreAuthorize("hasAuthority('coursepkg:write')")
    public ApiResponse<CoursePackageResponse> update(
            @PathVariable Long id, @Valid @RequestBody CoursePackageRequest request) {
        return ApiResponse.ok(coursePackageService.update(id, request));
    }

    @DeleteMapping("/api/packages/{id}")
    @PreAuthorize("hasAuthority('coursepkg:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        coursePackageService.delete(id);
        return ApiResponse.ok(null);
    }
}
