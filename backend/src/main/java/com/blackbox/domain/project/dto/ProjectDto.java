package com.blackbox.domain.project.dto;

import com.blackbox.domain.project.entity.ProjectMember.ProjectRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ProjectDto {

    public record CreateRequest(
            @NotBlank @Size(max = 100) String name,
            String description,
            String courseName,
            String semester,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record UpdateRequest(
            @Size(max = 100) String name,
            String description,
            String courseName,
            String semester,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record ProjectResponse(
            Long id,
            String name,
            String description,
            String courseName,
            String semester,
            LocalDate startDate,
            LocalDate endDate,
            String inviteCode,
            boolean active,
            int memberCount,
            Instant createdAt
    ) {}

    public record ProjectDetailResponse(
            Long id,
            String name,
            String description,
            String courseName,
            String semester,
            LocalDate startDate,
            LocalDate endDate,
            String inviteCode,
            boolean active,
            List<MemberResponse> members,
            Instant createdAt
    ) {}

    public record MemberResponse(
            Long userId,
            String name,
            String email,
            String role,
            String projectRole,
            boolean dataCollectionConsent,
            boolean consentGithub,
            boolean consentDrive,
            boolean consentAi,
            Instant joinedAt
    ) {}

    public record JoinRequest(
            @NotBlank String inviteCode,
            boolean dataCollectionConsent
    ) {}

    public record UpdateRoleRequest(
            @NotBlank String projectRole
    ) {}

    public record UpdateConsentRequest(
            boolean consentGithub,
            boolean consentDrive,
            boolean consentAi
    ) {}
}
