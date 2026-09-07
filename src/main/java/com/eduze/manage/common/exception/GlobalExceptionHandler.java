package com.eduze.manage.common.exception;

import com.eduze.manage.common.web.ApiError;
import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.common.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(f -> fieldErrors.put(
                f.getField(),
                f.getDefaultMessage() != null ? f.getDefaultMessage() : "Invalid"));
        String detail = fieldErrors.values().stream().findFirst().orElse(null);
        ApiError apiError = ApiError.builder()
                .message(detail != null ? detail : ErrorCode.VALIDATION_FAILED.getMessage())
                .fieldErrors(fieldErrors)
                .build();
        ApiResponse<ApiError> body = ApiResponse.fail(ErrorCode.VALIDATION_FAILED, apiError, currentTraceId());
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus()).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.FORBIDDEN, null, currentTraceId()));
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Object>> handleBiz(BizException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ApiResponse<Object> body = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(ex.getMessage())
                .data(null)
                .traceId(currentTraceId())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(ErrorCode.NOT_FOUND.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.NOT_FOUND, null, currentTraceId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleOther(Exception ex) {
        String traceId = currentTraceId();
        log.error("Unhandled exception traceId={}", traceId, ex);
        ApiResponse<Object> body = ApiResponse.fail(ErrorCode.INTERNAL_ERROR, null, traceId);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus()).body(body);
    }

    private static String currentTraceId() {
        String fromMdc = MDC.get(TraceIdFilter.TRACE_ID_KEY);
        if (fromMdc != null && !fromMdc.isBlank()) {
            return fromMdc;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
