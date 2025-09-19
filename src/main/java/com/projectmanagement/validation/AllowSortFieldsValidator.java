package com.projectmanagement.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.Set;

public class AllowSortFieldsValidator implements ConstraintValidator<AllowSortFields, Pageable> {

    private Set<String> allowedFields;

    @Override
    public void initialize(AllowSortFields constraintAnnotation) {
        this.allowedFields = Set.of(constraintAnnotation.value());
    }

    @Override
    public boolean isValid(Pageable pageable, ConstraintValidatorContext context) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return true;
        }

        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            if (!allowedFields.contains(property)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    String.format("Invalid sort field '%s'. Allowed fields: %s",
                        property, String.join(", ", allowedFields))
                ).addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}