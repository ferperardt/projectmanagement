package com.projectmanagement.task.dto;

import com.projectmanagement.task.enums.TaskPriority;
import com.projectmanagement.task.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTaskRequest(
    @NotBlank(message = "Task title is required")
    @Size(max = 200, message = "Task title must not exceed 200 characters")
    String title,

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    UUID assignedUserId,

    TaskStatus status,

    TaskPriority priority
) {}