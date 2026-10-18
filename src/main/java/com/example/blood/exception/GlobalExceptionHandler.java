package com.example.blood.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiErrorResponse.of(ex.getErrorCode(), ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        ErrorCode.VALIDATION_ERROR,
                        "Request validation failed",
                        Map.of("fields", fieldErrors)
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> violations = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> violations.put(v.getPropertyPath().toString(), v.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        ErrorCode.VALIDATION_ERROR,
                        "Invalid request parameters",
                        Map.of("violations", violations)
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof IllegalArgumentException
                || ex.getCause() instanceof InvalidFormatException
                || ex.getCause() instanceof ValueInstantiationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiErrorResponse.of(
                            ErrorCode.INVALID_ENUM_VALUE,
                            cause != null && cause.getMessage() != null ? cause.getMessage() : "Invalid field value in JSON body",
                            Map.of()
                    ));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        ErrorCode.MALFORMED_JSON,
                        "Malformed JSON request body",
                        Map.of()
                ));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ConversionFailedException.class})
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(Exception ex) {
        Throwable root = ex.getCause() != null ? ex.getCause() : ex;
        if (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage() != null ? root.getMessage() : "Invalid parameter value";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        ErrorCode.INVALID_ENUM_VALUE,
                        message,
                        Map.of()
                ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        ErrorCode.VALIDATION_ERROR,
                        "Missing required parameter: " + ex.getParameterName(),
                        Map.of("parameter", ex.getParameterName())
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        ErrorCode.BAD_REQUEST,
                        ex.getMessage() != null ? ex.getMessage() : "Invalid argument",
                        Map.of()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        // Never log PII or expose internal stack traces in API responses
        log.error("Unexpected error processing request: {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(
                        ErrorCode.INTERNAL_ERROR,
                        "An unexpected internal error occurred",
                        Map.of()
                ));
    }
}
