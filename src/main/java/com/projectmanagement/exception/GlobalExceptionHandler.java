package com.projectmanagement.exception;

import com.projectmanagement.exception.dto.ErrorResponse;
import com.projectmanagement.exception.dto.ValidationErrorResponse;
import com.projectmanagement.exception.dto.ValidationErrorResponse.FieldError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex, WebRequest request) {
        log.warn("User already exists: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            "Conflict",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(InvalidUserDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUserData(InvalidUserDataException ex, WebRequest request) {
        log.warn("Invalid user data: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            "Bad Request",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("HTTP message not readable: {}", ex.getMessage());
        
        String message = "Invalid request format";
        
        // Tratamento específico para erros de enum
        if (ex.getCause() instanceof InvalidFormatException invalidFormatEx) {
            if (invalidFormatEx.getTargetType() != null && invalidFormatEx.getTargetType().isEnum()) {
                Object[] enumConstants = invalidFormatEx.getTargetType().getEnumConstants();
                String validValues = String.join(", ", 
                    java.util.Arrays.stream(enumConstants)
                        .map(Object::toString)
                        .toArray(String[]::new)
                );
                message = String.format("Invalid value '%s' for field '%s'. Valid values are: [%s]",
                    invalidFormatEx.getValue(),
                    invalidFormatEx.getPath().get(0).getFieldName(),
                    validValues
                );
            }
        }
        
        ErrorResponse errorResponse = ErrorResponse.of(
            "Bad Request",
            message,
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation failed: {} errors", ex.getBindingResult().getFieldErrorCount());

        List<FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new FieldError(
                error.getField(),
                error.getRejectedValue(),
                error.getDefaultMessage()
            ))
            .toList();

        ValidationErrorResponse errorResponse = ValidationErrorResponse.of(
            "Request validation failed",
            request.getDescription(false).replace("uri=", ""),
            fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());

        String message = ex.getConstraintViolations()
            .stream()
            .map(ConstraintViolation::getMessage)
            .findFirst()
            .orElse("Validation error");

        ErrorResponse errorResponse = ErrorResponse.of(
            "Bad Request",
            message,
            request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage());
        
        String message = "Data integrity violation";
        String error = "Conflict";
        
        // Tenta extrair informação mais específica sobre a violação de constraint
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("users_username_key")) {
                message = "Username already exists";
            } else if (ex.getMessage().contains("users_email_key")) {
                message = "Email already exists";
            }
        }
        
        ErrorResponse errorResponse = ErrorResponse.of(
            error,
            message,
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        log.warn("Authentication failed: Invalid credentials provided");
        
        ErrorResponse errorResponse = ErrorResponse.of(
            "Unauthorized",
            "Invalid email or password",
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
            "Forbidden",
            "Access denied. You do not have permission to perform this action.",
            request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex, WebRequest request) {
        log.warn("Invalid refresh token: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
            "Unauthorized",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(ExpiredRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleExpiredRefreshToken(ExpiredRefreshTokenException ex, WebRequest request) {
        log.warn("Expired refresh token: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
            "Unauthorized",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}