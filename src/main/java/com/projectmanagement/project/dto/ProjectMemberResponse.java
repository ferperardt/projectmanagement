package com.projectmanagement.project.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectMemberResponse(
    UUID userId,
    String username,
    String email,
    String role,
    LocalDateTime joinedAt
) {}