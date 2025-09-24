package com.projectmanagement.task.dto;

import com.projectmanagement.project.dto.ProjectMemberResponse;
import com.projectmanagement.task.enums.TaskPriority;
import com.projectmanagement.task.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskDetailResponse(
    UUID id,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    UUID projectId,
    UUID assignedUserId,
    UUID createdById,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    ProjectMemberResponse assignedMember
) {}