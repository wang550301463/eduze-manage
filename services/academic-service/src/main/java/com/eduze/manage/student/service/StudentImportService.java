package com.eduze.manage.student.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.directory.BranchDirectory;
import com.eduze.manage.directory.BranchView;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.dto.GuardianUpsertRequest;
import com.eduze.manage.student.dto.StudentImportResult;
import com.eduze.manage.student.dto.StudentImportRow;
import com.eduze.manage.student.dto.StudentRequest;
import com.eduze.manage.student.mapper.StudentMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StudentImportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StudentService studentService;
    private final StudentMapper studentMapper;
    private final GuardianService guardianService;
    private final BranchDirectory branchDirectory;

    public void writeTemplate(HttpServletResponse response) throws IOException {
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = URLEncoder.encode("student-import-template", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".xlsx\"");
        EasyExcel.write(response.getOutputStream(), StudentImportRow.class)
                .sheet("学员导入")
                .doWrite(List.of());
    }

    public StudentImportResult importExcel(MultipartFile file) throws IOException {
        Map<String, Long> branchByCode =
                branchDirectory.all().stream()
                        .collect(
                                Collectors.toMap(
                                        BranchView::getCode, BranchView::getId, (a, b) -> a));

        List<StudentImportRow> rows = new ArrayList<>();
        try (InputStream in = file.getInputStream()) {
            EasyExcel.read(
                            in,
                            StudentImportRow.class,
                            new ReadListener<StudentImportRow>() {
                                @Override
                                public void invoke(StudentImportRow row, AnalysisContext context) {
                                    rows.add(row);
                                }

                                @Override
                                public void doAfterAllAnalysed(AnalysisContext context) {}
                            })
                    .sheet()
                    .doRead();
        }

        List<StudentImportResult.ImportFailure> failures = new ArrayList<>();
        int successCount = 0;
        int rowNum = 1;
        for (StudentImportRow row : rows) {
            rowNum++;
            List<StudentImportResult.FieldError> errors = validateRow(row, branchByCode);
            if (!errors.isEmpty()) {
                failures.add(
                        StudentImportResult.ImportFailure.builder()
                                .rowIndex(rowNum)
                                .errors(errors)
                                .build());
                continue;
            }
            try {
                importRow(row, branchByCode);
                successCount++;
            } catch (Exception ex) {
                failures.add(
                        StudentImportResult.ImportFailure.builder()
                                .rowIndex(rowNum)
                                .errors(
                                        List.of(
                                                StudentImportResult.FieldError.builder()
                                                        .column("_")
                                                        .message(
                                                                ex.getMessage() != null
                                                                        ? ex.getMessage()
                                                                        : "导入失败")
                                                        .build()))
                                .build());
            }
        }

        return StudentImportResult.builder().successCount(successCount).failures(failures).build();
    }

    private List<StudentImportResult.FieldError> validateRow(
            StudentImportRow row, Map<String, Long> branchByCode) {
        List<StudentImportResult.FieldError> errors = new ArrayList<>();
        if (!StringUtils.hasText(row.getEnrollNo())) {
            errors.add(field("enrollNo", "入园编号不能为空"));
        }
        if (!StringUtils.hasText(row.getName())) {
            errors.add(field("name", "姓名不能为空"));
        }
        if (!StringUtils.hasText(row.getBranchCode())
                || !branchByCode.containsKey(row.getBranchCode().trim())) {
            errors.add(field("branchCode", "校区编码无效"));
        }
        if (!StringUtils.hasText(row.getMentorTeacherId())) {
            errors.add(field("mentorTeacherId", "主带老师ID不能为空"));
        } else {
            try {
                Long.parseLong(row.getMentorTeacherId().trim());
            } catch (NumberFormatException ex) {
                errors.add(field("mentorTeacherId", "主带老师ID格式无效"));
            }
        }
        if (StringUtils.hasText(row.getEnrollNo())) {
            long count =
                    studentMapper.selectCount(
                            Wrappers.<Student>lambdaQuery()
                                    .eq(Student::getEnrollNo, row.getEnrollNo().trim()));
            if (count > 0) {
                errors.add(field("enrollNo", "入园编号已存在"));
            }
        }
        return errors;
    }

    private void importRow(StudentImportRow row, Map<String, Long> branchByCode) {
        StudentRequest request = new StudentRequest();
        request.setBranchId(branchByCode.get(row.getBranchCode().trim()));
        request.setEnrollNo(row.getEnrollNo().trim());
        request.setName(row.getName().trim());
        request.setGender(parseGender(row.getGender()));
        request.setBirthday(parseDate(row.getBirthday()));
        request.setEnrollDate(parseDate(row.getEnrollDate()));
        request.setAllergy(row.getAllergy());
        request.setHealthNote(row.getNote());
        request.setMentorTeacherId(Long.parseLong(row.getMentorTeacherId().trim()));
        var created = studentService.create(request);

        if (StringUtils.hasText(row.getGuardianPhone())) {
            GuardianUpsertRequest guardian = new GuardianUpsertRequest();
            guardian.setName(
                    StringUtils.hasText(row.getGuardianName())
                            ? row.getGuardianName().trim()
                            : "家长");
            guardian.setPhone(row.getGuardianPhone().trim());
            guardian.setRelation(
                    StringUtils.hasText(row.getRelation()) ? row.getRelation().trim() : "家长");
            guardian.setIsMainContact(1);
            guardianService.upsertForStudent(created.getId(), guardian);
        }
    }

    private StudentImportResult.FieldError field(String column, String message) {
        return StudentImportResult.FieldError.builder().column(column).message(message).build();
    }

    private Integer parseGender(String raw) {
        if (!StringUtils.hasText(raw)) {
            return 0;
        }
        return switch (raw.trim()) {
            case "男", "1" -> 1;
            case "女", "2" -> 2;
            default -> 0;
        };
    }

    private LocalDate parseDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
