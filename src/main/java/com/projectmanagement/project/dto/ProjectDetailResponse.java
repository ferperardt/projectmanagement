package com.projectmanagement.project.dto;

import com.projectmanagement.task.dto.TaskResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProjectDetailResponse(
    UUID id,
    String name,
    String description,
    UUID ownerId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<ProjectMemberResponse> members,
    List<TaskResponse> tasks
) {}