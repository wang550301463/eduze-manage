package com.eduze.manage.student.controller;

import com.eduze.manage.audit.AuditAction;
import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.student.dto.StudentImportResult;
import com.eduze.manage.student.service.StudentImportService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/students/import")
@RequiredArgsConstructor
public class StudentImportController {

    private final StudentImportService studentImportService;

    @GetMapping("/template")
    @PreAuthorize("hasAuthority('student:import')")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        studentImportService.writeTemplate(response);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('student:import')")
    @AuditAction(action = "STUDENT_IMPORT", entityType = "student")
    public ApiResponse<StudentImportResult> importExcel(@RequestParam("file") MultipartFile file)
            throws IOException {
        return ApiResponse.ok(studentImportService.importExcel(file));
    }
}
