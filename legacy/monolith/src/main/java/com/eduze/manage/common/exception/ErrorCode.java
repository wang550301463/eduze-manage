package com.eduze.manage.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    OK(0, "OK", HttpStatus.OK),
    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "Forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "Not Found", HttpStatus.NOT_FOUND),
    VALIDATION_FAILED(40001, "请检查填写内容", HttpStatus.BAD_REQUEST),
    CONFLICT(40901, "Conflict", HttpStatus.CONFLICT),
    UNPROCESSABLE(42200, "Unprocessable", HttpStatus.UNPROCESSABLE_ENTITY),
    TENANT_UNIQUE_VIOLATION(40902, "Tenant unique violation", HttpStatus.CONFLICT),
    ACCOUNT_LOCKED(42301, "账号已锁定，请稍后再试", HttpStatus.LOCKED),
    TOO_MANY_REQUESTS(42900, "请求过于频繁", HttpStatus.TOO_MANY_REQUESTS),
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
