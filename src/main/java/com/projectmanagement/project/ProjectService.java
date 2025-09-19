package com.projectmanagement.project;

import com.projectmanagement.project.dto.CreateProjectRequest;
import com.projectmanagement.project.dto.ProjectResponse;
import com.projectmanagement.project.enums.ProjectMemberRole;
import com.projectmanagement.user.User;
import com.projectmanagement.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMapper projectMapper;
    private final UserService userService;

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

    public ProjectResponse getProjectResponse(Project project) {
        return projectMapper.toResponse(project);
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