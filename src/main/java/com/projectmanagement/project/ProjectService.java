package com.projectmanagement.project;

import com.projectmanagement.auth.CustomUserDetails;
import com.projectmanagement.exception.InsufficientProjectPermissionException;
import com.projectmanagement.exception.ProjectMembershipException;
import com.projectmanagement.exception.ProjectNotFoundException;
import com.projectmanagement.exception.UserNotFoundException;
import com.projectmanagement.project.dto.*;
import com.projectmanagement.project.enums.ProjectMemberRole;
import com.projectmanagement.task.TaskMapper;
import com.projectmanagement.task.TaskRepository;
import com.projectmanagement.task.dto.TaskResponse;
import com.projectmanagement.user.User;
import com.projectmanagement.user.UserRepository;
import com.projectmanagement.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserRepository userRepository;
    private final UserService userService;
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Transactional
    public Project createProject(CreateProjectRequest request, Authentication authentication) {
        String userEmail = authentication.getName();
        log.debug("Creating project '{}' for user: {}", request.name(), userEmail);

        User owner = userService.findByEmail(userEmail);


        Project project = projectMapper.toEntity(request);
        project.setOwnerId(owner.getId());

        Project savedProject = projectRepository.save(project);
        log.info("Project created successfully with ID: {}", savedProject.getId());

        createOwnerMembership(savedProject, owner);
        log.info("Owner membership created for project: {}", savedProject.getId());

        return savedProject;
    }

    public Page<ProjectResponse> getUserProjects(Authentication authentication, Pageable pageable) {
        UUID userId = CustomUserDetails.getUserId(authentication);
        log.debug("Getting projects for user ID: {}", userId);

        Page<Project> projects = projectRepository.findProjectsByUserId(userId, pageable);

        log.info("Found {} projects for user ID: {}", projects.getTotalElements(), userId);
        return projects.map(projectMapper::toResponse);
    }

    public ProjectDetailResponse getProjectDetails(UUID projectId, Authentication authentication) {
        UUID currentUserId = CustomUserDetails.getUserId(authentication);
        log.debug("Getting project details for project: {} by user: {}", projectId, authentication.getName());

        validateUserIsProjectMember(currentUserId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        List<ProjectMemberView> memberViews = projectMemberRepository.findProjectMembersWithUsers(projectId);
        List<ProjectMemberResponse> memberResponses = memberViews.stream()
                .map(projectMemberMapper::toResponse)
                .toList();

        List<TaskResponse> tasks = taskRepository.findByProjectId(projectId).stream()
                .map(taskMapper::toResponse)
                .toList();

        ProjectDetailResponse response = projectMapper.toDetailResponse(project, memberResponses, tasks);
        log.info("Retrieved project details for project: {} with {} members and {} tasks",
                projectId, memberResponses.size(), tasks.size());

        return response;
    }

    private void validateUserIsProjectMember(UUID userId, UUID projectId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("User {} is not a member of project {}", userId, projectId);
            throw new ProjectMembershipException(userId.toString());
        }
    }

    @Transactional
    public void addMemberToProject(UUID projectId, AddMemberRequest request, Authentication authentication) {
        UUID currentUserId = CustomUserDetails.getUserId(authentication);
        log.debug("Adding member {} with role {} to project {} by user {}",
                request.userId(), request.role(), projectId, authentication.getName());

        validateProjectExists(projectId);
        validateUserIsProjectMember(currentUserId, projectId);
        validateUserHasPermissionToAddMembers(currentUserId, projectId);
        validateTargetUserExists(request.userId());

        Optional<ProjectMember> existingMember = projectMemberRepository.findByProjectIdAndUserId(projectId, request.userId());

        if (existingMember.isPresent()) {
            ProjectMember member = existingMember.get();
            if (member.getRole() != request.role()) {
                member.setRole(request.role());
                projectMemberRepository.save(member);
                log.info("Updated member {} role to {} in project {}", request.userId(), request.role(), projectId);
            } else {
                log.debug("Member {} already has role {} in project {}", request.userId(), request.role(), projectId);
            }
        } else {
            ProjectMember projectMember = new ProjectMember();
            projectMember.setProjectId(projectId);
            projectMember.setUserId(request.userId());
            projectMember.setRole(request.role());
            projectMember.setJoinedAt(LocalDateTime.now());

            projectMemberRepository.save(projectMember);
            log.info("Added new member {} with role {} to project {}", request.userId(), request.role(), projectId);
        }
    }

    private void validateProjectExists(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }
    }

    private void validateUserHasPermissionToAddMembers(UUID userId, UUID projectId) {
        List<ProjectMemberRole> allowedRoles = List.of(ProjectMemberRole.OWNER, ProjectMemberRole.MANAGER);
        boolean hasPermission = projectMemberRepository.existsByProjectIdAndUserIdAndRoleIn(projectId, userId, allowedRoles);

        if (!hasPermission) {
            throw new InsufficientProjectPermissionException();
        }
    }

    private void validateTargetUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
    }

    @Transactional
    public void updateProject(UUID projectId, UpdateProjectRequest request, Authentication authentication) {
        UUID currentUserId = CustomUserDetails.getUserId(authentication);
        log.debug("Updating project {} by user: {}", projectId, authentication.getName());

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        validateUserIsProjectMember(currentUserId, projectId);

        project.setName(request.name());
        project.setDescription(request.description());

        projectRepository.save(project);

        log.info("Project {} updated successfully by user: {}", projectId, authentication.getName());
    }

    private void createOwnerMembership(Project project, User owner) {
        ProjectMember projectMember = new ProjectMember();
        projectMember.setProjectId(project.getId());
        projectMember.setUserId(owner.getId());
        projectMember.setRole(ProjectMemberRole.OWNER);
        projectMember.setJoinedAt(LocalDateTime.now());

        projectMemberRepository.save(projectMember);
        log.debug("Created OWNER membership for user {} in project {}", owner.getId(), project.getId());
    }
}