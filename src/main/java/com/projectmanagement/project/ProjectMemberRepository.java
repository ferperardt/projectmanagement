package com.projectmanagement.project;

import com.projectmanagement.project.dto.ProjectMemberView;
import com.projectmanagement.project.enums.ProjectMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    List<ProjectMember> findByProjectId(UUID projectId);

    List<ProjectMember> findByUserId(UUID userId);

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserIdAndRole(UUID projectId, UUID userId, ProjectMemberRole role);

    @Query("SELECT u.id as userId, u.username as username, u.email as email, pm.role as role, pm.joinedAt as joinedAt FROM ProjectMember pm JOIN User u ON pm.userId = u.id WHERE pm.projectId = :projectId")
    List<ProjectMemberView> findProjectMembersWithUsers(@Param("projectId") UUID projectId);

    boolean existsByProjectIdAndUserIdAndRoleIn(UUID projectId, UUID userId, List<ProjectMemberRole> roles);
}