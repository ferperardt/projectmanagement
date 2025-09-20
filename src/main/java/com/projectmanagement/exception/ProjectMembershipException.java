package com.projectmanagement.exception;

public class ProjectMembershipException extends BusinessException {

    public ProjectMembershipException(String userId) {
        super(String.format("User %s must be a member of the project", userId));
    }

    public ProjectMembershipException(String message, Throwable cause) {
        super(message, cause);
    }
}