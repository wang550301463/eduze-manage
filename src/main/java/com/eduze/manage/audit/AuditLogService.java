package com.eduze.manage.audit;

import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

    private static final String TRACE_ID_KEY = "traceId";

    private final AuditLogMapper auditLogMapper;
    private final SpelExpressionParser spelParser = new SpelExpressionParser();

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void record(ProceedingJoinPoint joinPoint, AuditAction auditAction, Object result, Throwable error) {
        AuditLog log = new AuditLog();
        log.setTenantId(TenantContext.getTenantId());
        log.setAction(auditAction.action());
        log.setEntityType(auditAction.entityType());
        log.setEntityId(resolveEntityId(joinPoint, auditAction.entityIdSpEL()));
        log.setStatus(error == null ? "success" : "fail");
        log.setErrorMsg(error == null ? null : truncate(error.getMessage(), 512));
        log.setCreatedAt(LocalDateTime.now());
        log.setExtraJson("{\"traceId\":\"" + resolveTraceId() + "\"}");

        fillCurrentUser(log);

        HttpServletRequest request = currentRequest();
        if (request != null) {
            log.setIp(resolveClientIp(request));
            log.setUserAgent(truncate(request.getHeader("User-Agent"), 256));
            log.setRequestPath(request.getRequestURI());
        }

        auditLogMapper.insert(log);
    }

    private void fillCurrentUser(AuditLog log) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            return;
        }
        log.setUserId(details.getUserId());
        log.setUsername(details.getUsername());
    }

    private static String resolveTraceId() {
        String fromMdc = MDC.get(TRACE_ID_KEY);
        if (fromMdc != null && !fromMdc.isBlank()) {
            return fromMdc;
        }
        String generated = UUID.randomUUID().toString().replace("-", "");
        MDC.put(TRACE_ID_KEY, generated);
        return generated;
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return truncate(first, 64);
            }
        }
        return truncate(request.getRemoteAddr(), 64);
    }

    private String resolveEntityId(ProceedingJoinPoint joinPoint, String entityIdSpEL) {
        if (entityIdSpEL == null || entityIdSpEL.isBlank()) {
            return null;
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        Expression expression = spelParser.parseExpression(entityIdSpEL);
        Object value = expression.getValue(context);
        return value == null ? null : String.valueOf(value);
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
