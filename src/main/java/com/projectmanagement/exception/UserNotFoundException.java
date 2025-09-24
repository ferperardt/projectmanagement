package com.projectmanagement.exception;

import java.util.UUID;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(UUID userId) {
        super(String.format("User with ID %s not found", userId));
    }

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}