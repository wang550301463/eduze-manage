package com.eduze.manage.student.controller;

import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.course.dto.AddMembersRequest;
import com.eduze.manage.course.service.ClassGroupMemberService;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/bulk")
@RequiredArgsConstructor
public class StudentBulkController {

    private final ClassGroupMemberService classGroupMemberService;

    @PostMapping("/assign-class")
    @PreAuthorize("hasAuthority('classgroup:assign')")
    public ApiResponse<Void> assignClass(@Valid @RequestBody AssignClassRequest request) {
        AddMembersRequest add = new AddMembersRequest();
        add.setStudentIds(request.getStudentIds());
        classGroupMemberService.addMembers(request.getClassGroupId(), add);
        return ApiResponse.ok(null);
    }

    @Getter
    @Setter
    public static class AssignClassRequest {
        private Long classGroupId;
        private java.util.List<Long> studentIds;
    }
}
