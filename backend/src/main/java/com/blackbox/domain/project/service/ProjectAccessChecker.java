package com.blackbox.domain.project.service;

import com.blackbox.domain.project.entity.Project;
import com.blackbox.domain.project.entity.ProjectMember;
import com.blackbox.domain.project.entity.ProjectMember.ProjectRole;
import com.blackbox.domain.project.repository.ProjectMemberRepository;
import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.global.exception.BusinessException;
import com.blackbox.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAccessChecker {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    /**
     * 프로젝트 조회 및 접근 권한 확인 (멤버 여부 or PROFESSOR/TA)
     */
    public Project getAccessibleProject(Long projectId, User requester) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        User.Role role = requester.getRole();
        if (role == User.Role.PROFESSOR || role == User.Role.TA) {
            return project; // 교수/TA는 모든 프로젝트 열람 가능
        }

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, requester.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return project;
    }

    /**
     * LEADER 권한 확인
     */
    public void requireLeader(Long projectId, User requester) {
        ProjectMember member = projectMemberRepository
                .findByProjectIdAndUserId(projectId, requester.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));

        if (member.getProjectRole() != ProjectRole.LEADER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * 멤버 참조 (없으면 예외)
     */
    public ProjectMember getMember(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
