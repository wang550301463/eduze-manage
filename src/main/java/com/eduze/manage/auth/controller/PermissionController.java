package com.eduze.manage.auth.controller;

import com.eduze.manage.auth.domain.Permission;
import com.eduze.manage.auth.service.PermissionService;
import com.eduze.manage.common.web.ApiResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public ApiResponse<Map<String, List<Permission>>> list() {
        return ApiResponse.ok(permissionService.listGroupedByModule());
    }
}
