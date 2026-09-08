package com.eduze.manage.config;

import com.eduze.platform.runtime.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(basePackages = "com.eduze.manage")
@Order(-1)
public class PlatformErrors {
    @ExceptionHandler(PlatformException.class)
    public ResponseEntity<ApiResponse<Void>> error(PlatformException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getStatus(), ex.getMessage()));
    }
}
