package com.helma.helmabackend.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.helma.helmabackend.controller.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<ApiError> handleFieldValidation(FieldValidationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, ex.getFieldErrors());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req, null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof InvalidFormatException invalidFormat) {
            Map<String, String> fieldErrors = new LinkedHashMap<>();
            String field = toFieldPath(invalidFormat);
            if (field != null && !field.isBlank()) {
                fieldErrors.put(field, messageForInvalidFormat(invalidFormat));
            }
            return build(
                    HttpStatus.BAD_REQUEST,
                    fieldErrors.isEmpty() ? "Invalid request body (malformed JSON)" : "Validation failed",
                    req,
                    fieldErrors.isEmpty() ? null : fieldErrors
            );
        }

        if (cause instanceof UnrecognizedPropertyException unknownProperty) {
            String field = unknownProperty.getPropertyName();
            Map<String, String> fieldErrors = field == null || field.isBlank()
                    ? null
                    : Map.of(field, "Unknown field.");
            return build(
                    HttpStatus.BAD_REQUEST,
                    fieldErrors == null ? "Invalid request body (malformed JSON)" : "Validation failed",
                    req,
                    fieldErrors
            );
        }

        if (cause instanceof MismatchedInputException mismatchedInput) {
            String field = toFieldPath(mismatchedInput);
            Map<String, String> fieldErrors = new LinkedHashMap<>();
            if (field != null && !field.isBlank()) {
                fieldErrors.put(field, "Value has the wrong JSON shape or type.");
            }
            return build(
                    HttpStatus.BAD_REQUEST,
                    fieldErrors.isEmpty() ? "Invalid request body (malformed JSON)" : "Validation failed",
                    req,
                    fieldErrors.isEmpty() ? null : fieldErrors
            );
        }

        if (cause instanceof JsonParseException) {
            return build(HttpStatus.BAD_REQUEST, "Invalid request body (malformed JSON)", req, null);
        }

        return build(HttpStatus.BAD_REQUEST, "Invalid request body (malformed JSON)", req, null);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntime(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }

    private String toFieldPath(MismatchedInputException ex) {
        if (ex.getPath() == null || ex.getPath().isEmpty()) {
            return null;
        }

        StringBuilder path = new StringBuilder();
        ex.getPath().forEach(ref -> {
            if (ref.getFieldName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                path.append('[').append(ref.getIndex()).append(']');
            }
        });
        return path.toString();
    }

    private String messageForInvalidFormat(InvalidFormatException ex) {
        Class<?> targetType = ex.getTargetType();

        if (targetType != null && targetType.isEnum()) {
            String allowed = Arrays.stream(targetType.getEnumConstants())
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            return "Must be one of: " + allowed;
        }

        if (targetType == Integer.class || targetType == int.class
                || targetType == Long.class || targetType == long.class
                || targetType == Double.class || targetType == double.class
                || targetType == Float.class || targetType == float.class
                || targetType == Short.class || targetType == short.class
                || targetType == Byte.class || targetType == byte.class
                || targetType == java.math.BigDecimal.class) {
            return "Must be a valid number.";
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            return "Must be true or false.";
        }

        return "Invalid value.";
    }
}
