package com.projectmanagement.task;

import com.projectmanagement.task.dto.CreateTaskRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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