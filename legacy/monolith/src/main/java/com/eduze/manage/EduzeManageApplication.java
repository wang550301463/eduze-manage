package com.eduze.manage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan({
    "com.eduze.manage.auth.mapper",
    "com.eduze.manage.branch.mapper",
    "com.eduze.manage.student.mapper",
    "com.eduze.manage.course.mapper",
    "com.eduze.manage.lesson.mapper",
    "com.eduze.manage.attendance.mapper",
    "com.eduze.manage.audit",
    "com.eduze.manage.curriculum.mapper",
    "com.eduze.manage.teacher.mapper"
})
@EnableScheduling
public class EduzeManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduzeManageApplication.class, args);
    }
}
