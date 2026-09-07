package com.eduze.manage.audit;

import org.springframework.stereotype.Service;

@Service
public class AuditTestService {

    @AuditAction(action = "STUDENT_READ", entityType = "student", entityIdSpEL = "#id")
    public String readStudent(Long id) {
        return "ok-" + id;
    }
}
