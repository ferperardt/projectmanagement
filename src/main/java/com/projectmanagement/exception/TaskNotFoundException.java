package com.projectmanagement.exception;

import java.util.UUID;

public class TaskNotFoundException extends BusinessException {

    public TaskNotFoundException(UUID taskId) {
        super(String.format("Task not found with ID: %s", taskId));
    }

    public TaskNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}