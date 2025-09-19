package com.projectmanagement.project.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    String name,
    String description,
    UUID ownerId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}