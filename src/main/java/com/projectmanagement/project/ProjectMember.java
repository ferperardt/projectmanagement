package com.projectmanagement.project;

import com.projectmanagement.project.enums.ProjectMemberRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_members")
@IdClass(ProjectMemberId.class)
@Data
@EqualsAndHashCode(of = {"projectId", "userId"})
public class ProjectMember {

    @Id
    @Column(nullable = false)
    private UUID projectId;

    @Id
    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectMemberRole role;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}