package com.projectmanagement.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    String email,
    String role,
    Boolean enabled,
    Boolean twoFactorEnabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}