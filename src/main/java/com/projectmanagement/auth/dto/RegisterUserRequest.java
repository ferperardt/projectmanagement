package com.projectmanagement.auth.dto;

import com.projectmanagement.user.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @NotNull(message = "Role is required")
        UserRole role,

        Boolean enabled,

        Boolean twoFactorEnabled,

        String twoFactorSecret
) {
}