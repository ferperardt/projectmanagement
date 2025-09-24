package com.projectmanagement.project.dto;

import com.projectmanagement.project.enums.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddMemberRequest(
    @NotNull(message = "User ID is required")
    UUID userId,

    @NotNull(message = "Role is required")
    ProjectMemberRole role
) {}