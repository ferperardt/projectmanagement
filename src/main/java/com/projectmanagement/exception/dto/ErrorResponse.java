package com.projectmanagement.exception.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ErrorResponse(
        String error,
        String message,
        String path,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp
) {
    public static ErrorResponse of(String error, String message, String path) {
        return new ErrorResponse(error, message, path, LocalDateTime.now());
    }
}