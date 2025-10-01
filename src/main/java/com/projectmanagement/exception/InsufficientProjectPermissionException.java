package com.projectmanagement.exception;

public class InsufficientProjectPermissionException extends BusinessException {


    public InsufficientProjectPermissionException(String message) {
        super(message);
    }

    public InsufficientProjectPermissionException(String message, Throwable cause) {
        super(message, cause);
    }
}