package com.projectmanagement.exception;

public class InvalidSortParameterException extends BusinessException {

    public InvalidSortParameterException(String field, String allowedFields) {
        super("Invalid sort parameter: '%s'. Allowed fields: %s".formatted(field, allowedFields));
    }
}