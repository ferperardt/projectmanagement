package com.projectmanagement.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByOwnerId(UUID ownerId);

    boolean existsByNameAndOwnerId(String name, UUID ownerId);

    @Query("SELECT p FROM Project p JOIN ProjectMember pm ON p.id = pm.projectId WHERE pm.userId = :userId")
    Page<Project> findProjectsByUserId(@Param("userId") UUID userId, Pageable pageable);
}