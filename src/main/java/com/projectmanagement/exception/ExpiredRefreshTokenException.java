package com.projectmanagement.exception;

public class ExpiredRefreshTokenException extends BusinessException {

    public ExpiredRefreshTokenException(String message) {
        super(message);
    }

    public ExpiredRefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}