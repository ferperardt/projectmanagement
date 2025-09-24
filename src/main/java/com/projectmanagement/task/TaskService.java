package com.projectmanagement.task;

import com.projectmanagement.auth.CustomUserDetails;
import com.projectmanagement.exception.ProjectMembershipException;
import com.projectmanagement.exception.TaskNotFoundException;
import com.projectmanagement.project.ProjectMemberRepository;
import com.projectmanagement.task.dto.AssignTaskRequest;
import com.projectmanagement.task.dto.CreateTaskRequest;
import com.projectmanagement.task.dto.TaskDetailResponse;
import com.projectmanagement.task.dto.TaskResponse;
import com.projectmanagement.task.enums.TaskPriority;
import com.projectmanagement.task.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public Page<TaskResponse> getProjectTasks(UUID projectId, Authentication authentication, Pageable pageable) {
        UUID currentUserId = CustomUserDetails.getUserId(authentication);
        log.debug("Fetching tasks for project: {} by user: {}", projectId, authentication.getName());

        validateUserIsProjectMember(currentUserId, projectId);

        Page<Task> tasks = taskRepository.findByProjectId(projectId, pageable);
        return tasks.map(taskMapper::toResponse);
    }

    public TaskResponse getTaskResponse(Task task) {
        return taskMapper.toResponse(task);
    }

    @Transactional
    public void assignTask(UUID taskId, AssignTaskRequest request, Authentication authentication) {
        UUID currentUserId = CustomUserDetails.getUserId(authentication);
        log.debug("Assigning task {} to user {} by user: {}", taskId, request.assignedUserId(), authentication.getName());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        validateUserIsProjectMember(currentUserId, task.getProjectId());

        if (request.assignedUserId() != null) {
            validateUserIsProjectMember(request.assignedUserId(), task.getProjectId());
        }

        task.setAssignedUserId(request.assignedUserId());
        taskRepository.save(task);

        log.info("Task {} assigned to user {} successfully", taskId, request.assignedUserId());
    }

    public TaskDetailResponse getTaskDetails(UUID taskId, Authentication authentication) {
        UUID currentUserId = CustomUserDetails.getUserId(authentication);
        log.debug("Fetching task details for task: {} by user: {}", taskId, authentication.getName());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        validateUserIsProjectMember(currentUserId, task.getProjectId());

        if (task.getAssignedUserId() == null) {
            return taskMapper.toTaskDetailResponse(task, null);
        }

        return projectMemberRepository.findProjectMemberWithUser(task.getProjectId(), task.getAssignedUserId())
                .map(memberView -> taskMapper.toTaskDetailResponse(task, memberView))
                .orElseThrow(() -> new ProjectMembershipException("Assigned user " + task.getAssignedUserId() + " is not a member of the project"));
    }

    private void validateUserIsProjectMember(UUID userId, UUID projectId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("User {} is not a member of project {}", userId, projectId);
            throw new ProjectMembershipException(userId.toString());
        }
    }

}