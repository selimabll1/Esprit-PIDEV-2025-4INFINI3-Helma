package com.helma.helmabackend.exception;

import java.util.LinkedHashMap;
import java.util.Map;

public class FieldValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public FieldValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? Map.of() : new LinkedHashMap<>(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public static FieldValidationException single(String field, String message) {
        return new FieldValidationException("Validation failed", Map.of(field, message));
    }
}
