package com.blackbox.domain.project.dto;

import com.blackbox.domain.project.entity.ProjectMember.ProjectRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class ProjectDto {

    public record CreateRequest(
            @NotBlank @Size(max = 100) String name,
            String description
    ) {}

    public record UpdateRequest(
            @Size(max = 100) String name,
            String description
    ) {}

    public record ProjectResponse(
            Long id,
            String name,
            String description,
            String inviteCode,
            boolean active,
            int memberCount,
            Instant createdAt
    ) {}

    public record ProjectDetailResponse(
            Long id,
            String name,
            String description,
            String inviteCode,
            boolean active,
            List<MemberResponse> members,
            Instant createdAt
    ) {}

    public record MemberResponse(
            Long userId,
            String name,
            String email,
            String role,           // User.Role (STUDENT/PROFESSOR/TA)
            String projectRole,    // ProjectRole (LEADER/MEMBER/OBSERVER)
            boolean dataCollectionConsent,
            Instant joinedAt
    ) {}

    public record JoinRequest(
            @NotBlank String inviteCode,
            boolean dataCollectionConsent
    ) {}

    public record UpdateRoleRequest(
            @NotBlank String projectRole
    ) {}
}
