package com.eduze.manage.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.auth.security.CustomUserDetails;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Import(AuditTestService.class)
class AuditAspectIT extends AbstractITContainerTest {

    @Autowired
    private AuditTestService auditTestService;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @BeforeEach
    void cleanAuditLogs() {
        auditLogMapper.delete(null);
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void auditedMethod_persistsAuditLog() {
        long before = auditLogMapper.selectCount(null);

        String result = auditTestService.readStudent(42L);
        assertEquals("ok-42", result);

        long after = auditLogMapper.selectCount(null);
        assertEquals(before + 1, after);

        AuditLog log = auditLogMapper.selectOne(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getAction, "STUDENT_READ")
                .orderByDesc(AuditLog::getCreatedAt)
                .last("LIMIT 1"));
        assertEquals("student", log.getEntityType());
        assertEquals("42", log.getEntityId());
        assertEquals("success", log.getStatus());
        assertEquals(1L, log.getTenantId());
        assertTrue(log.getCreatedAt() != null);
    }

    @Test
    void auditedMethod_recordsCurrentUserAndForwardedIpAndTraceId() {
        CustomUserDetails details = new CustomUserDetails(
                9001L,
                1L,
                "auditor",
                "Auditor",
                "",
                List.of(1L),
                Set.of("ADMIN"),
                List.of("student:read"),
                1,
                true);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        request.setRequestURI("/api/students/42");
        request.addHeader("User-Agent", "AuditIT/1.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.put("traceId", "trace-audit-it-001");

        auditTestService.readStudent(42L);

        AuditLog log = auditLogMapper.selectOne(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getAction, "STUDENT_READ")
                .orderByDesc(AuditLog::getCreatedAt)
                .last("LIMIT 1"));
        assertNotNull(log);
        assertEquals(9001L, log.getUserId());
        assertEquals("auditor", log.getUsername());
        assertEquals("203.0.113.10", log.getIp());
        assertNotNull(log.getExtraJson());
        assertTrue(log.getExtraJson().contains("trace-audit-it-001"));
    }
}
