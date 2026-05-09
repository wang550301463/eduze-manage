package com.eduze.manage.common.exception;

import com.eduze.manage.common.web.ApiError;
import com.eduze.manage.common.web.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(f -> fieldErrors.put(
                f.getField(),
                f.getDefaultMessage() != null ? f.getDefaultMessage() : "Invalid"));
        ApiError apiError = ApiError.builder()
                .message(ErrorCode.VALIDATION_FAILED.getMessage())
                .fieldErrors(fieldErrors)
                .build();
        ApiResponse<ApiError> body = ApiResponse.fail(ErrorCode.VALIDATION_FAILED, apiError);
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus()).body(body);
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Object>> handleBiz(BizException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ApiResponse<Object> body = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(ex.getMessage())
                .data(null)
                .traceId(null)
                .build();
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleOther(Exception ex) {
        ApiResponse<Object> body = ApiResponse.fail(ErrorCode.INTERNAL_ERROR, null);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus()).body(body);
    }
}
