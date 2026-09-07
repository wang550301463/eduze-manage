package com.eduze.manage.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(auditAction)")
    public Object around(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            error = throwable;
            throw throwable;
        } finally {
            try {
                auditLogService.record(joinPoint, auditAction, result, error);
            } catch (Exception auditFailure) {
                log.warn("Failed to persist audit log for action={}", auditAction.action(), auditFailure);
            }
        }
    }
}
