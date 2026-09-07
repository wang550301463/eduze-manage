package com.eduze.manage.student.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduze.manage.audit.AuditAction;
import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.common.web.PageResult;
import com.eduze.manage.student.dto.AssignMentorRequest;
import com.eduze.manage.student.dto.GuardianResponse;
import com.eduze.manage.student.dto.GuardianSummaryResponse;
import com.eduze.manage.student.dto.GuardianUpsertRequest;
import com.eduze.manage.student.dto.LessonHourAdjustRequest;
import com.eduze.manage.student.dto.LinkGuardianRequest;
import com.eduze.manage.student.dto.MentorHistoryResponse;
import com.eduze.manage.student.dto.StageAssessmentRequest;
import com.eduze.manage.student.dto.StageAssessmentResponse;
import com.eduze.manage.student.dto.StudentLessonHourLedgerResponse;
import com.eduze.manage.student.dto.StudentRequest;
import com.eduze.manage.student.dto.StudentResponse;
import com.eduze.manage.student.dto.StudentStatusRequest;
import com.eduze.manage.student.dto.StudentUpdateRequest;
import com.eduze.manage.student.service.GuardianService;
import com.eduze.manage.student.service.StageAssessmentService;
import com.eduze.manage.student.service.StudentGuardianService;
import com.eduze.manage.student.service.StudentLessonHourLedgerService;
import com.eduze.manage.student.service.StudentMentorService;
import com.eduze.manage.student.service.StudentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentGuardianService studentGuardianService;
    private final GuardianService guardianService;
    private final StudentMentorService studentMentorService;
    private final StageAssessmentService stageAssessmentService;
    private final StudentLessonHourLedgerService lessonHourLedgerService;

    @GetMapping
    @PreAuthorize("hasAuthority('student:read')")
    public ApiResponse<PageResult<StudentResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long classGroupId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer pkgRemainingMax,
            @RequestParam(required = false) String mask) {
        boolean maskPhone = "phone".equals(mask);
        Page<StudentResponse> result =
                studentService.list(keyword, branchId, classGroupId, status, pkgRemainingMax, maskPhone, page, size);
        return ApiResponse.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('student:read')")
    @AuditAction(action = "STUDENT_READ", entityType = "student", entityIdSpEL = "#id")
    public ApiResponse<StudentResponse> get(
            @PathVariable Long id, @RequestParam(required = false) String mask) {
        return ApiResponse.ok(studentService.get(id, "phone".equals(mask)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('student:write')")
    @AuditAction(action = "STUDENT_CREATE", entityType = "student")
    public ApiResponse<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        return ApiResponse.ok(studentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('student:write')")
    @AuditAction(action = "STUDENT_UPDATE", entityType = "student", entityIdSpEL = "#id")
    public ApiResponse<StudentResponse> update(
            @PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
        return ApiResponse.ok(studentService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('student:write')")
    public ApiResponse<StudentResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody StudentStatusRequest request) {
        return ApiResponse.ok(studentService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('student:delete')")
    @AuditAction(action = "STUDENT_DELETE", entityType = "student", entityIdSpEL = "#id")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{studentId}/guardians")
    @PreAuthorize("hasAuthority('guardian:read')")
    public ApiResponse<List<GuardianSummaryResponse>> guardians(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "false") boolean pickupOnly) {
        return ApiResponse.ok(studentGuardianService.listByStudent(studentId, pickupOnly));
    }

    @PostMapping("/{studentId}/guardians/upsert")
    @PreAuthorize("hasAuthority('guardian:write')")
    public ApiResponse<GuardianResponse> upsertGuardian(
            @PathVariable Long studentId, @Valid @RequestBody GuardianUpsertRequest request) {
        return ApiResponse.ok(guardianService.upsertForStudent(studentId, request));
    }

    @PostMapping("/{studentId}/guardians/{guardianId}")
    @PreAuthorize("hasAuthority('guardian:write')")
    public ApiResponse<GuardianResponse> linkGuardian(
            @PathVariable Long studentId,
            @PathVariable Long guardianId,
            @Valid @RequestBody LinkGuardianRequest request) {
        return ApiResponse.ok(guardianService.linkToStudent(studentId, guardianId, request));
    }

    @PutMapping("/{id}/mentor")
    @PreAuthorize("hasAuthority('student:mentor_assign')")
    public ApiResponse<Void> changeMentor(
            @PathVariable Long id, @Valid @RequestBody AssignMentorRequest req) {
        studentMentorService.changeMentor(id, req);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/mentor-history")
    @PreAuthorize("hasAuthority('student:read')")
    public ApiResponse<List<MentorHistoryResponse>> mentorHistory(@PathVariable Long id) {
        return ApiResponse.ok(studentMentorService.history(id));
    }

    @PostMapping("/{id}/stage-assessments")
    @PreAuthorize("hasAuthority('student:write')")
    public ApiResponse<StageAssessmentResponse> createAssessment(
            @PathVariable Long id, @Valid @RequestBody StageAssessmentRequest req) {
        return ApiResponse.ok(stageAssessmentService.create(id, req));
    }

    @GetMapping("/{id}/stage-assessments")
    @PreAuthorize("hasAuthority('student:read')")
    public ApiResponse<List<StageAssessmentResponse>> listAssessments(@PathVariable Long id) {
        return ApiResponse.ok(stageAssessmentService.list(id));
    }

    @GetMapping("/{id}/lesson-hour-ledger")
    @PreAuthorize("hasAuthority('student:read')")
    public ApiResponse<List<StudentLessonHourLedgerResponse>> listLessonHourLedger(@PathVariable Long id) {
        return ApiResponse.ok(lessonHourLedgerService.listByStudent(id));
    }

    @PostMapping("/{id}/lesson-hour-ledger/adjust")
    @PreAuthorize("hasAuthority('student:hour_adjust')")
    public ApiResponse<StudentLessonHourLedgerResponse> adjustLessonHours(
            @PathVariable Long id, @Valid @RequestBody LessonHourAdjustRequest request) {
        return ApiResponse.ok(lessonHourLedgerService.adjust(id, request));
    }
}
