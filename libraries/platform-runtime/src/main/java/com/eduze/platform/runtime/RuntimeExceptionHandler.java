package com.eduze.platform.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        basePackages = {
            "com.eduze.platform",
            "com.eduze.teaching",
            "com.eduze.portfolio",
            "com.eduze.engagement",
            "com.eduze.commerce"
        })
public class RuntimeExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeExceptionHandler.class);

    @ExceptionHandler(PlatformException.class)
    public ResponseEntity<ApiResponse<Void>> business(PlatformException error) {
        return ResponseEntity.status(error.getStatus())
                .body(ApiResponse.error(error.getStatus(), error.getMessage()));
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        jakarta.validation.ConstraintViolationException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> validation(Exception error) {
        return ResponseEntity.badRequest().body(ApiResponse.error(400, "请检查填写内容"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception error) {
        // Do not log exception message: JDBC/remote errors can contain submitted personal data.
        LOGGER.error("Request failed: {}", error.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(ApiResponse.error(500, "处理失败，请稍后重试"));
    }
}
