package com.projectmanagement.task;

import com.projectmanagement.task.dto.AssignTaskRequest;
import com.projectmanagement.task.dto.TaskDetailResponse;
import com.projectmanagement.task.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Validated
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/{id}")
    public ResponseEntity<TaskDetailResponse> getTaskDetails(
            @PathVariable UUID id,
            Authentication authentication) {

        TaskDetailResponse taskDetails = taskService.getTaskDetails(id, authentication);
        return ResponseEntity.ok(taskDetails);
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<Void> assignTask(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTaskRequest request,
            Authentication authentication) {

        taskService.assignTask(id, request, authentication);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication) {

        taskService.updateTask(id, request, authentication);

        URI location = URI.create("/api/tasks/" + id);
        return ResponseEntity.noContent().location(location).build();
    }
}