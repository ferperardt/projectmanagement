package com.projectmanagement.project;

import com.projectmanagement.project.dto.CreateProjectRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

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