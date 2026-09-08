package com.eduze.manage.auth.controller;

import com.eduze.manage.audit.AuditAction;
import com.eduze.manage.auth.dto.AssignBranchesRequest;
import com.eduze.manage.auth.dto.AssignRolesRequest;
import com.eduze.manage.auth.dto.UserCreateRequest;
import com.eduze.manage.auth.dto.UserResponse;
import com.eduze.manage.auth.dto.UserUpdateRequest;
import com.eduze.manage.auth.service.UserService;
import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.common.web.PageResult;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ApiResponse<PageResult<UserResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(userService.page(page, size, keyword));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:write')")
    @AuditAction(action = "USER_CREATE", entityType = "user")
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:write')")
    @AuditAction(action = "USER_UPDATE", entityType = "user", entityIdSpEL = "#id")
    public ApiResponse<UserResponse> update(
            @PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('role:assign')")
    @AuditAction(action = "USER_ASSIGN_ROLES", entityType = "user", entityIdSpEL = "#id")
    public ApiResponse<Void> assignRoles(
            @PathVariable Long id, @Valid @RequestBody AssignRolesRequest request) {
        userService.assignRoles(id, request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/branches")
    @PreAuthorize("hasAuthority('user:write')")
    @AuditAction(action = "USER_ASSIGN_BRANCHES", entityType = "user", entityIdSpEL = "#id")
    public ApiResponse<Void> assignBranches(
            @PathVariable Long id, @Valid @RequestBody AssignBranchesRequest request) {
        userService.assignBranches(id, request);
        return ApiResponse.ok(null);
    }
}
