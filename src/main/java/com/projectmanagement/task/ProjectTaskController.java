package com.projectmanagement.task;

import com.projectmanagement.task.dto.CreateTaskRequest;
import com.projectmanagement.task.dto.TaskResponse;
import com.projectmanagement.validation.AllowSortFields;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
@Validated
public class ProjectTaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getProjectTasks(
            @PathVariable UUID projectId,
            Authentication authentication,
            @AllowSortFields({"id", "title", "status", "priority", "createdAt", "updatedAt", "assignedUserId", "createdById"}) Pageable pageable) {

        Page<TaskResponse> tasks = taskService.getProjectTasks(projectId, authentication, pageable);
        return ResponseEntity.ok(tasks);
    }

    @PostMapping
    public ResponseEntity<Void> createTask(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTaskRequest createTaskRequest,
            Authentication authentication) {

        Task createdTask = taskService.createTask(createTaskRequest, projectId, authentication);
        URI location = URI.create("/api/tasks/" + createdTask.getId());

        return ResponseEntity.created(location).build();
    }
}