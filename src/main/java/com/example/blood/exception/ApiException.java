package com.example.blood.exception;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus status;
    private final Map<String, Object> details;

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, defaultStatus(errorCode), message, Collections.emptyMap());
    }

    public ApiException(ErrorCode errorCode, HttpStatus status, String message) {
        this(errorCode, status, message, Collections.emptyMap());
    }

    public ApiException(ErrorCode errorCode, HttpStatus status, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
        this.details = details != null ? details : Collections.emptyMap();
    }

    private static HttpStatus defaultStatus(ErrorCode code) {
        if (code == null) {
            return HttpStatus.BAD_REQUEST;
        }
        return switch (code) {
            case IDENTITY_REQUIRED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, HOSPITAL_NOT_VERIFIED -> HttpStatus.FORBIDDEN;
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE_RESOURCE, INVALID_STATE_TRANSITION -> HttpStatus.CONFLICT;
            case INELIGIBLE_DONOR, INCOMPATIBLE_BLOOD_GROUP, VALIDATION_ERROR, INVALID_ENUM_VALUE, MALFORMED_JSON, BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
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
