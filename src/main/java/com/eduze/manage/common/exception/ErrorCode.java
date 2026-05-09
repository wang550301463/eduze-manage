package com.eduze.manage.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    OK(0, "OK", HttpStatus.OK),
    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "Forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "Not Found", HttpStatus.NOT_FOUND),
    VALIDATION_FAILED(40001, "Validation failed", HttpStatus.BAD_REQUEST),
    CONFLICT(40901, "Conflict", HttpStatus.CONFLICT),
    TENANT_UNIQUE_VIOLATION(40902, "Tenant unique violation", HttpStatus.CONFLICT),
    INTERNAL_ERROR(500, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
