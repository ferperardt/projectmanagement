package com.projectmanagement.project;

import com.projectmanagement.project.dto.CreateProjectRequest;
import com.projectmanagement.project.dto.ProjectDetailResponse;
import com.projectmanagement.project.dto.ProjectMemberResponse;
import com.projectmanagement.project.dto.ProjectResponse;
import com.projectmanagement.task.dto.TaskResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProjectMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Project toEntity(CreateProjectRequest request);

    ProjectResponse toResponse(Project project);

    @Mapping(source = "project.id", target = "id")
    @Mapping(source = "project.name", target = "name")
    @Mapping(source = "project.description", target = "description")
    @Mapping(source = "project.ownerId", target = "ownerId")
    @Mapping(source = "project.createdAt", target = "createdAt")
    @Mapping(source = "project.updatedAt", target = "updatedAt")
    @Mapping(source = "members", target = "members")
    @Mapping(source = "tasks", target = "tasks")
    ProjectDetailResponse toDetailResponse(Project project, List<ProjectMemberResponse> members, List<TaskResponse> tasks);
}