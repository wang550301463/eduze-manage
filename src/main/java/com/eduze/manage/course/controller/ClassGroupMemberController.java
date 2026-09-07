package com.eduze.manage.course.controller;

import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.course.dto.AddMembersRequest;
import com.eduze.manage.course.dto.ClassMemberResponse;
import com.eduze.manage.course.dto.TransferClassRequest;
import com.eduze.manage.course.service.ClassGroupMemberService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ClassGroupMemberController {

    private final ClassGroupMemberService classGroupMemberService;

    @GetMapping("/api/class-groups/{id}/members")
    @PreAuthorize("hasAuthority('classgroup:read')")
    public ApiResponse<List<ClassMemberResponse>> listMembers(
            @PathVariable Long id, @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ApiResponse.ok(classGroupMemberService.listMembers(id, activeOnly));
    }

    @PostMapping("/api/class-groups/{id}/members")
    @PreAuthorize("hasAuthority('classgroup:assign')")
    public ApiResponse<Void> addMembers(@PathVariable Long id, @Valid @RequestBody AddMembersRequest request) {
        classGroupMemberService.addMembers(id, request);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/api/class-groups/{id}/members/{studentId}")
    @PreAuthorize("hasAuthority('classgroup:assign')")
    public ApiResponse<Void> removeMember(@PathVariable Long id, @PathVariable Long studentId) {
        classGroupMemberService.removeMember(id, studentId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/students/bulk/transfer-class")
    @PreAuthorize("hasAuthority('classgroup:assign')")
    public ApiResponse<Void> transfer(@Valid @RequestBody TransferClassRequest request) {
        classGroupMemberService.transfer(request);
        return ApiResponse.ok(null);
    }
}
