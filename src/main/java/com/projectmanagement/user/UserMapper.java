package com.projectmanagement.user;

import com.projectmanagement.auth.dto.RegisterUserRequest;
import com.projectmanagement.user.dto.UserResponse;
import com.projectmanagement.user.enums.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "enabled", defaultValue = "true")
    @Mapping(target = "twoFactorEnabled", defaultValue = "false")
    User toEntity(RegisterUserRequest request);

    @Mapping(source = "role", target = "role")
    UserResponse toResponse(User user);

    default String mapRole(UserRole role) {
        return role != null ? role.name() : null;
    }
}