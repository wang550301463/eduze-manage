package com.eduze.manage.student.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentImportRow {

    @ExcelProperty("入园编号")
    private String enrollNo;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("生日")
    private String birthday;

    @ExcelProperty("入园日期")
    private String enrollDate;

    @ExcelProperty("校区编码")
    private String branchCode;

    @ExcelProperty("主带老师ID")
    private String mentorTeacherId;

    @ExcelProperty("主家长姓名")
    private String guardianName;

    @ExcelProperty("主家长手机")
    private String guardianPhone;

    @ExcelProperty("关系")
    private String relation;

    @ExcelProperty("过敏史")
    private String allergy;

    @ExcelProperty("备注")
    private String note;
}
