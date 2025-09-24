package com.projectmanagement.task;

import com.projectmanagement.project.dto.ProjectMemberResponse;
import com.projectmanagement.project.dto.ProjectMemberView;
import com.projectmanagement.task.dto.TaskDetailResponse;
import com.projectmanagement.task.dto.TaskResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TaskMapper {

    TaskResponse toResponse(Task task);

    @Mapping(source = "memberView", target = "assignedMember")
    TaskDetailResponse toTaskDetailResponse(Task task, ProjectMemberView memberView);

    ProjectMemberResponse toProjectMemberResponse(ProjectMemberView memberView);
}