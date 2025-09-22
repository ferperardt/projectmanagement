package com.projectmanagement.exception;

import java.util.UUID;

public class ProjectNotFoundException extends BusinessException {

    public ProjectNotFoundException(UUID projectId) {
        super(String.format("Project not found with ID: %s", projectId));
    }

    public ProjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}