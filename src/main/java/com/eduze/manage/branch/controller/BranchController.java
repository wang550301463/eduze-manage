package com.eduze.manage.branch.controller;

import com.eduze.manage.branch.dto.BranchRequest;
import com.eduze.manage.branch.dto.BranchResponse;
import com.eduze.manage.branch.service.BranchService;
import com.eduze.manage.common.web.ApiResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @PreAuthorize("hasAuthority('branch:read')")
    public ApiResponse<List<BranchResponse>> list() {
        return ApiResponse.ok(branchService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('branch:read')")
    public ApiResponse<BranchResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(branchService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('branch:write')")
    public ApiResponse<BranchResponse> create(@Valid @RequestBody BranchRequest request) {
        return ApiResponse.ok(branchService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('branch:write')")
    public ApiResponse<BranchResponse> update(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return ApiResponse.ok(branchService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('branch:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        branchService.delete(id);
        return ApiResponse.ok(null);
    }
}
