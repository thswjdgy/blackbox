package com.blackbox.domain.project.service;

import com.blackbox.domain.project.dto.ProjectDto;
import com.blackbox.domain.project.entity.Project;
import com.blackbox.domain.project.entity.ProjectMember;
import com.blackbox.domain.project.entity.ProjectMember.ProjectRole;
import com.blackbox.domain.project.repository.ProjectMemberRepository;
import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.global.exception.BusinessException;
import com.blackbox.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAccessChecker accessChecker;

    /** 내 프로젝트 목록 */
    public List<ProjectDto.ProjectResponse> getMyProjects(User requester) {
        return projectRepository.findAllByMemberUserId(requester.getId())
                .stream()
                .map(p -> toResponse(p))
                .toList();
    }

    /** 프로젝트 상세 */
    public List<ProjectDto.MemberResponse> getMembers(Long projectId, User requester) {
        Project project = accessChecker.getAccessibleProject(projectId, requester);
        return toDetailResponse(project).members();
    }

    public ProjectDto.ProjectDetailResponse getProject(Long projectId, User requester) {
        Project project = accessChecker.getAccessibleProject(projectId, requester);
        return toDetailResponse(project);
    }

    /** 프로젝트 생성 */
    @Transactional
    public ProjectDto.ProjectDetailResponse createProject(ProjectDto.CreateRequest req, User requester) {
        String inviteCode = generateUniqueInviteCode();

        Project project = Project.builder()
                .name(req.name())
                .description(req.description())
                .inviteCode(inviteCode)
                .build();
        projectRepository.save(project);

        // 생성자는 LEADER
        ProjectMember leader = ProjectMember.builder()
                .project(project)
                .user(requester)
                .projectRole(ProjectRole.LEADER)
                .dataCollectionConsent(true)
                .consentAt(Instant.now())
                .build();
        project.getMembers().add(leader);
        projectMemberRepository.save(leader);

        return toDetailResponse(project);
    }

    /** 프로젝트 수정 (LEADER만) */
    @Transactional
    public ProjectDto.ProjectDetailResponse updateProject(Long projectId,
                                                          ProjectDto.UpdateRequest req,
                                                          User requester) {
        accessChecker.requireLeader(projectId, requester);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        if (req.name() != null) project.setName(req.name());
        if (req.description() != null) project.setDescription(req.description());

        return toDetailResponse(project);
    }

    /** 초대 코드로 프로젝트 참여 */
    @Transactional
    public ProjectDto.ProjectDetailResponse joinByInviteCode(ProjectDto.JoinRequest req, User requester) {
        Project project = projectRepository.findByInviteCode(req.inviteCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_CODE_NOT_FOUND));

        if (projectMemberRepository.existsByProjectIdAndUserId(project.getId(), requester.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_PROJECT_MEMBER);
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(requester)
                .projectRole(ProjectRole.MEMBER)
                .dataCollectionConsent(req.dataCollectionConsent())
                .consentAt(req.dataCollectionConsent() ? Instant.now() : null)
                .build();
        project.getMembers().add(member);
        projectMemberRepository.save(member);

        return toDetailResponse(project);
    }

    /** 멤버 역할 변경 (LEADER만) */
    @Transactional
    public void updateMemberRole(Long projectId, Long targetUserId,
                                  ProjectDto.UpdateRoleRequest req, User requester) {
        accessChecker.requireLeader(projectId, requester);
        ProjectMember member = accessChecker.getMember(projectId, targetUserId);

        try {
            member.setProjectRole(ProjectRole.valueOf(req.projectRole().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 역할입니다: " + req.projectRole());
        }
    }

    /** 프로젝트 탈퇴 (LEADER는 불가) */
    @Transactional
    public void leaveProject(Long projectId, User requester) {
        ProjectMember member = accessChecker.getMember(projectId, requester.getId());

        if (member.getProjectRole() == ProjectRole.LEADER) {
            throw new BusinessException(ErrorCode.LEADER_CANNOT_LEAVE);
        }
        projectMemberRepository.delete(member);
    }

    /** 멤버 강퇴 (LEADER만) */
    @Transactional
    public void removeMember(Long projectId, Long targetUserId, User requester) {
        accessChecker.requireLeader(projectId, requester);
        ProjectMember member = accessChecker.getMember(projectId, targetUserId);
        projectMemberRepository.delete(member);
    }

    // ─── 매핑 헬퍼 ───────────────────────────────────────────

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (projectRepository.findByInviteCode(code).isPresent());
        return code;
    }

    private ProjectDto.ProjectResponse toResponse(Project p) {
        return new ProjectDto.ProjectResponse(
                p.getId(), p.getName(), p.getDescription(),
                p.getInviteCode(), p.isActive(), p.getMembers().size(), p.getCreatedAt());
    }

    private ProjectDto.ProjectDetailResponse toDetailResponse(Project p) {
        List<ProjectDto.MemberResponse> members = p.getMembers().stream()
                .map(m -> new ProjectDto.MemberResponse(
                        m.getUser().getId(),
                        m.getUser().getName(),
                        m.getUser().getEmail(),
                        m.getUser().getRole().name(),
                        m.getProjectRole().name(),
                        m.isDataCollectionConsent(),
                        m.getJoinedAt()))
                .toList();

        return new ProjectDto.ProjectDetailResponse(
                p.getId(), p.getName(), p.getDescription(),
                p.getInviteCode(), p.isActive(), members, p.getCreatedAt());
    }
}
