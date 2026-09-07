package com.eduze.manage.student.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.common.web.PageResult;
import com.eduze.manage.student.dto.StudentLessonHistoryResponse;
import com.eduze.manage.student.service.StudentLessonHistoryService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student-lesson-histories")
@RequiredArgsConstructor
public class StudentLessonHistoryController {

    private final StudentLessonHistoryService studentLessonHistoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('student:read')")
    public ApiResponse<PageResult<StudentLessonHistoryResponse>> list(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<StudentLessonHistoryResponse> result =
                studentLessonHistoryService.list(studentId, branchId, from, to, page, size);
        return ApiResponse.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }
}
