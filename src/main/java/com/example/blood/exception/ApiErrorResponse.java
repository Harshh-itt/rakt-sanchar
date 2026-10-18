package com.example.blood.exception;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        String code,
        String message,
        Map<String, Object> details,
        Instant timestamp
) {
    public static ApiErrorResponse of(ErrorCode code, String message, Map<String, Object> details) {
        return new ApiErrorResponse(
                code.name(),
                message,
                details != null ? details : Map.of(),
                Instant.now()
        );
    }
}
