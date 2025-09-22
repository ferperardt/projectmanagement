package com.projectmanagement.project.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ProjectMemberView {
    UUID getUserId();
    String getUsername();
    String getEmail();
    String getRole();
    LocalDateTime getJoinedAt();
}