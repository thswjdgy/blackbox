package com.blackbox.domain.github.service;

import com.blackbox.domain.github.dto.GitHubDto;
import com.blackbox.domain.github.entity.GitHubInstallation;
import com.blackbox.domain.github.entity.GitHubUserMapping;
import com.blackbox.domain.github.repository.GitHubInstallationRepository;
import com.blackbox.domain.github.repository.GitHubUserMappingRepository;
import com.blackbox.domain.project.entity.Project;
import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.domain.user.repository.UserRepository;
import com.blackbox.global.exception.BusinessException;
import com.blackbox.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GitHubService {

    private final GitHubInstallationRepository installationRepository;
    private final GitHubUserMappingRepository  mappingRepository;
    private final ProjectRepository            projectRepository;
    private final UserRepository               userRepository;

    /** 레포 연동 (upsert) */
    @Transactional
    public GitHubDto.InstallationResponse link(Long projectId, GitHubDto.LinkRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        GitHubInstallation inst = installationRepository.findByProjectId(projectId)
                .orElseGet(() -> GitHubInstallation.builder().project(project).build());

        inst.setRepoFullName(req.repoFullName());
        if (req.githubToken() != null && !req.githubToken().isBlank())
            inst.setGithubToken(req.githubToken());
        if (req.webhookSecret() != null && !req.webhookSecret().isBlank())
            inst.setWebhookSecret(req.webhookSecret());

        installationRepository.save(inst);
        return toInstallationResponse(inst);
    }

    /** 연동 해제 */
    @Transactional
    public void unlink(Long projectId) {
        installationRepository.findByProjectId(projectId)
                .ifPresent(installationRepository::delete);
    }

    /** 연동 정보 조회 */
    public GitHubDto.InstallationResponse getInstallation(Long projectId) {
        return installationRepository.findByProjectId(projectId)
                .map(this::toInstallationResponse)
                .orElse(null);
    }

    /** 유저 매핑 추가 */
    @Transactional
    public GitHubDto.MappingResponse addMapping(Long projectId, GitHubDto.MappingRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (mappingRepository.existsByProjectIdAndUserId(projectId, req.userId())) {
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN); // 이미 매핑됨
        }

        GitHubUserMapping mapping = GitHubUserMapping.builder()
                .project(project)
                .user(user)
                .githubLogin(req.githubLogin())
                .build();
        mappingRepository.save(mapping);
        return toMappingResponse(mapping);
    }

    /** 유저 매핑 삭제 */
    @Transactional
    public void deleteMapping(Long projectId, Long mappingId) {
        mappingRepository.deleteByProjectIdAndId(projectId, mappingId);
    }

    /** 유저 매핑 목록 */
    public List<GitHubDto.MappingResponse> getMappings(Long projectId) {
        return mappingRepository.findByProjectId(projectId).stream()
                .map(this::toMappingResponse).toList();
    }

    /* ── 변환 ── */
    private GitHubDto.InstallationResponse toInstallationResponse(GitHubInstallation i) {
        return new GitHubDto.InstallationResponse(
                i.getId(), i.getProject().getId(),
                i.getRepoFullName(),
                i.getGithubToken() != null,
                i.getLastPolledAt(),
                i.getConnectedAt()
        );
    }

    private GitHubDto.MappingResponse toMappingResponse(GitHubUserMapping m) {
        return new GitHubDto.MappingResponse(
                m.getId(), m.getUser().getId(),
                m.getUser().getName(),
                m.getGithubLogin()
        );
    }
}
