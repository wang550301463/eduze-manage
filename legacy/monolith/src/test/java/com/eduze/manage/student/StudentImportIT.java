package com.eduze.manage.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alibaba.excel.EasyExcel;
import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.student.dto.StudentImportRow;
import com.eduze.manage.support.ApiITSupport;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class StudentImportIT extends AbstractITContainerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ApiITSupport api;

    @Autowired private JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM t_student_guardian_relation");
        jdbcTemplate.update("DELETE FROM t_guardian");
        jdbcTemplate.update(
                "DELETE FROM t_student WHERE enroll_no NOT LIKE 'DEMO%' OR enroll_no IS NULL");
        token = api.login("admin", "admin@123");
    }

    @Test
    void import_partialSuccess() throws Exception {
        List<StudentImportRow> rows = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            StudentImportRow row = new StudentImportRow();
            row.setEnrollNo("IMP-" + i);
            row.setName("导入学员" + i);
            row.setBranchCode(i <= 3 ? "HQ" : "INVALID");
            row.setMentorTeacherId("1101");
            row.setGuardianName("家长" + i);
            row.setGuardianPhone("1380001000" + i);
            row.setRelation("母亲");
            rows.add(row);
        }
        byte[] bytes = writeExcel(rows);
        MockMultipartFile file =
                new MockMultipartFile("file", "students.xlsx", "application/vnd.ms-excel", bytes);

        mockMvc.perform(multipart("/api/students/import").file(file).with(api.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(3))
                .andExpect(jsonPath("$.data.failures.length()").value(2));

        Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM t_student WHERE enroll_no LIKE 'IMP-%'",
                        Integer.class);
        assertEquals(3, count);

        Long mentor =
                jdbcTemplate.queryForObject(
                        "SELECT mentor_teacher_id FROM t_student WHERE enroll_no = 'IMP-1'",
                        Long.class);
        assertEquals(1101L, mentor);
    }

    @Test
    void import_missingMentor_reportsFailure() throws Exception {
        StudentImportRow row = new StudentImportRow();
        row.setEnrollNo("IMP-NO-MENTOR");
        row.setName("无主带学员");
        row.setBranchCode("HQ");
        byte[] bytes = writeExcel(List.of(row));
        MockMultipartFile file =
                new MockMultipartFile("file", "students.xlsx", "application/vnd.ms-excel", bytes);

        mockMvc.perform(multipart("/api/students/import").file(file).with(api.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failures.length()").value(1))
                .andExpect(
                        jsonPath("$.data.failures[0].errors[0].column").value("mentorTeacherId"));
    }

    private byte[] writeExcel(List<StudentImportRow> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, StudentImportRow.class).sheet("学员导入").doWrite(rows);
        return out.toByteArray();
    }
}
