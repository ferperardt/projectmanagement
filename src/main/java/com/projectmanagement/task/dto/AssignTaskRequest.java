package com.projectmanagement.task.dto;

import java.util.UUID;

public record AssignTaskRequest(
    UUID assignedUserId
) {}