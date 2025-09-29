package com.projectmanagement.task.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projectmanagement.task.enums.TaskPriority;
import com.projectmanagement.task.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateTaskRequest(
    @JsonProperty(required = true)
    @NotBlank(message = "Task title is required")
    @Size(max = 200, message = "Task title must not exceed 200 characters")
    String title,

    @JsonProperty(required = true)
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    @JsonProperty(required = true)
    @NotNull(message = "Task status is required")
    TaskStatus status,

    @JsonProperty(required = true)
    @NotNull(message = "Task priority is required")
    TaskPriority priority,

    @JsonProperty(required = true)
    UUID assignedUserId
) {}