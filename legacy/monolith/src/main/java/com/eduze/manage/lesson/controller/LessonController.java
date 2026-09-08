package com.eduze.manage.lesson.controller;

import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.lesson.dto.BulkGenerateRequest;
import com.eduze.manage.lesson.dto.BulkGenerateResult;
import com.eduze.manage.lesson.dto.CancelLessonRequest;
import com.eduze.manage.lesson.dto.ConflictCheckRequest;
import com.eduze.manage.lesson.dto.ConflictReport;
import com.eduze.manage.lesson.dto.LessonChangeLogResponse;
import com.eduze.manage.lesson.dto.LessonRequest;
import com.eduze.manage.lesson.dto.LessonResponse;
import com.eduze.manage.lesson.dto.LessonStudentRequest;
import com.eduze.manage.lesson.dto.LessonStudentResponse;
import com.eduze.manage.lesson.dto.RescheduleRequest;
import com.eduze.manage.lesson.service.LessonService;
import com.eduze.manage.lesson.service.LessonStudentService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final LessonStudentService lessonStudentService;

    @GetMapping
    @PreAuthorize("hasAuthority('lesson:read')")
    public ApiResponse<List<LessonResponse>> list(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long classGroupId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime to) {
        return ApiResponse.ok(lessonService.list(branchId, classGroupId, teacherId, from, to));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('lesson:read')")
    public ApiResponse<LessonResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(lessonService.get(id));
    }

    @GetMapping("/{id}/change-logs")
    @PreAuthorize("hasAuthority('lesson:read')")
    public ApiResponse<List<LessonChangeLogResponse>> changeLogs(@PathVariable Long id) {
        return ApiResponse.ok(lessonService.changeLogs(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('lesson:write')")
    public ApiResponse<LessonResponse> create(@Valid @RequestBody LessonRequest request) {
        return ApiResponse.ok(lessonService.create(request));
    }

    @PostMapping("/bulk-generate")
    @PreAuthorize("hasAuthority('lesson:write')")
    public ApiResponse<BulkGenerateResult> bulkGenerate(
            @Valid @RequestBody BulkGenerateRequest request) {
        return ApiResponse.ok(lessonService.bulkGenerate(request));
    }

    @PostMapping("/check-conflict")
    @PreAuthorize("hasAuthority('lesson:read')")
    public ApiResponse<ConflictReport> checkConflict(
            @Valid @RequestBody ConflictCheckRequest request) {
        return ApiResponse.ok(lessonService.checkConflict(request));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasAuthority('lesson:reschedule')")
    public ApiResponse<LessonResponse> reschedule(
            @PathVariable Long id, @Valid @RequestBody RescheduleRequest request) {
        return ApiResponse.ok(lessonService.reschedule(id, request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('lesson:cancel')")
    public ApiResponse<LessonResponse> cancel(
            @PathVariable Long id, @Valid @RequestBody CancelLessonRequest request) {
        return ApiResponse.ok(lessonService.cancel(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('lesson:cancel')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        lessonService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAuthority('lesson:read')")
    public ApiResponse<List<LessonStudentResponse>> listStudents(@PathVariable Long id) {
        return ApiResponse.ok(lessonStudentService.list(id));
    }

    @PostMapping("/{id}/students")
    @PreAuthorize("hasAuthority('lesson:write')")
    public ApiResponse<LessonStudentResponse> addStudent(
            @PathVariable Long id, @Valid @RequestBody LessonStudentRequest req) {
        return ApiResponse.ok(lessonStudentService.addStudent(id, req));
    }

    @DeleteMapping("/{id}/students/{studentId}")
    @PreAuthorize("hasAuthority('lesson:write')")
    public ApiResponse<Void> removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
        lessonStudentService.removeStudent(id, studentId);
        return ApiResponse.ok(null);
    }
}
