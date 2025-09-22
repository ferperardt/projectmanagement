package com.projectmanagement.project;

import com.projectmanagement.project.dto.ProjectMemberResponse;
import com.projectmanagement.project.dto.ProjectMemberView;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProjectMemberMapper {


    ProjectMemberResponse toResponse(ProjectMemberView view);

}