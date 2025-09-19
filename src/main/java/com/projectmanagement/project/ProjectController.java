package com.projectmanagement.project;

import com.projectmanagement.project.dto.CreateProjectRequest;
import com.projectmanagement.project.dto.ProjectResponse;
import com.projectmanagement.validation.AllowSortFields;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Validated
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<Page<ProjectResponse>> getUserProjects(
            Authentication authentication,
            @AllowSortFields({"id", "name", "description", "createdAt", "updatedAt"}) Pageable pageable) {

        Page<ProjectResponse> projects = projectService.getUserProjects(authentication, pageable);
        return ResponseEntity.ok(projects);
    }

    @PostMapping
    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    public ResponseEntity<Void> createProject(
            @Valid @RequestBody CreateProjectRequest createProjectRequest,
            Authentication authentication) {

        Project createdProject = projectService.createProject(createProjectRequest, authentication);
        URI location = URI.create("/api/projects/" + createdProject.getId());

        return ResponseEntity.created(location).build();
    }
}