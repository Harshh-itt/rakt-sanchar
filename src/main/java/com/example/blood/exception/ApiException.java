package com.example.blood.exception;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus status;
    private final Map<String, Object> details;

    public ApiException(ErrorCode errorCode, HttpStatus status, String message) {
        this(errorCode, status, message, Collections.emptyMap());
    }

    public ApiException(ErrorCode errorCode, HttpStatus status, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
        this.details = details != null ? details : Collections.emptyMap();
    }

    public static ApiException notFound(String message) {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException duplicate(String message) {
        return new ApiException(ErrorCode.DUPLICATE_RESOURCE, HttpStatus.CONFLICT, message);
    }

    public static ApiException identityRequired(String message) {
        return new ApiException(ErrorCode.IDENTITY_REQUIRED, HttpStatus.UNAUTHORIZED, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, message);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
