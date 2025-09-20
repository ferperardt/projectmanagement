package com.projectmanagement.task;

import com.projectmanagement.auth.CustomUserDetails;
import com.projectmanagement.exception.ProjectMembershipException;
import com.projectmanagement.project.ProjectMemberRepository;
import com.projectmanagement.task.dto.CreateTaskRequest;
import com.projectmanagement.task.dto.TaskResponse;
import com.projectmanagement.task.enums.TaskPriority;
import com.projectmanagement.task.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskMapper taskMapper;

    @Transactional
    public Task createTask(CreateTaskRequest request, UUID projectId, Authentication authentication) {
        UUID currentUserId = CustomUserDetails.getUserId(authentication);
        log.debug("Creating task '{}' for project: {} by user: {}", request.title(), projectId, authentication.getName());

        validateUserIsProjectMember(currentUserId, projectId);

        UUID assignedUserId = request.assignedUserId() != null ? request.assignedUserId() : currentUserId;

        if (!assignedUserId.equals(currentUserId)) {
            validateUserIsProjectMember(assignedUserId, projectId);
        }

        // Create task directly with all values
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setProjectId(projectId);
        task.setAssignedUserId(assignedUserId);
        task.setCreatedById(currentUserId);
        // Apply defaults for optional fields
        task.setStatus(request.status() != null ? request.status() : TaskStatus.TODO);
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.LOW);

        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully with ID: {} for project: {}", savedTask.getId(), savedTask.getProjectId());

        return savedTask;
    }

    public TaskResponse getTaskResponse(Task task) {
        return taskMapper.toResponse(task);
    }

    private void validateUserIsProjectMember(UUID userId, UUID projectId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("User {} is not a member of project {}", userId, projectId);
            throw new ProjectMembershipException(userId.toString());
        }
    }

}