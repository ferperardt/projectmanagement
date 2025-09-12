package com.projectmanagement.exception.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record ValidationErrorResponse(
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp
) {
    public static ValidationErrorResponse of(String message, String path, List<FieldError> fieldErrors) {
        return new ValidationErrorResponse("Validation Failed", message, path, fieldErrors, LocalDateTime.now());
    }
    
    public record FieldError(
            String field,
            Object rejectedValue,
            String message
    ) {}
}