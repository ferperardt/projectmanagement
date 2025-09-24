package com.projectmanagement.exception;

public class InsufficientProjectPermissionException extends BusinessException {

    public InsufficientProjectPermissionException() {
        super("Only project owners and managers can add members to the project");
    }

    public InsufficientProjectPermissionException(String message) {
        super(message);
    }

    public InsufficientProjectPermissionException(String message, Throwable cause) {
        super(message, cause);
    }
}