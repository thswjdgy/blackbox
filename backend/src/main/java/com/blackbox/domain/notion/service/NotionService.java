package com.blackbox.domain.notion.service;

import com.blackbox.domain.notion.dto.NotionDto;
import com.blackbox.domain.notion.entity.NotionInstallation;
import com.blackbox.domain.notion.entity.NotionUserMapping;
import com.blackbox.domain.notion.repository.NotionInstallationRepository;
import com.blackbox.domain.notion.repository.NotionUserMappingRepository;
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
public class NotionService {

    private final NotionInstallationRepository installationRepository;
    private final NotionUserMappingRepository  mappingRepository;
    private final ProjectRepository            projectRepository;
    private final UserRepository               userRepository;

    /** 연동 (upsert) */
    @Transactional
    public NotionDto.InstallationResponse link(Long projectId, NotionDto.LinkRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        NotionInstallation inst = installationRepository.findByProjectId(projectId)
                .orElseGet(() -> NotionInstallation.builder().project(project).build());

        inst.setIntegrationToken(req.integrationToken());
        if (req.databaseId()    != null && !req.databaseId().isBlank())
            inst.setDatabaseId(req.databaseId().trim());
        if (req.workspaceName() != null && !req.workspaceName().isBlank())
            inst.setWorkspaceName(req.workspaceName().trim());

        installationRepository.save(inst);
        return toResponse(inst);
    }

    /** 연동 해제 */
    @Transactional
    public void unlink(Long projectId) {
        installationRepository.findByProjectId(projectId)
                .ifPresent(installationRepository::delete);
    }

    /** 연동 정보 조회 */
    public NotionDto.InstallationResponse getInstallation(Long projectId) {
        return installationRepository.findByProjectId(projectId)
                .map(this::toResponse).orElse(null);
    }

    /** 유저 매핑 추가 */
    @Transactional
    public NotionDto.MappingResponse addMapping(Long projectId, NotionDto.MappingRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (mappingRepository.existsByProjectIdAndUserId(projectId, req.userId()))
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN);

        NotionUserMapping m = NotionUserMapping.builder()
                .project(project).user(user)
                .notionUserId(req.notionUserId())
                .notionUserName(req.notionUserName())
                .build();
        mappingRepository.save(m);
        return toMappingResponse(m);
    }

    /** 유저 매핑 삭제 */
    @Transactional
    public void deleteMapping(Long projectId, Long mappingId) {
        mappingRepository.deleteByProjectIdAndId(projectId, mappingId);
    }

    /** 유저 매핑 목록 */
    public List<NotionDto.MappingResponse> getMappings(Long projectId) {
        return mappingRepository.findByProjectId(projectId).stream()
                .map(this::toMappingResponse).toList();
    }

    private NotionDto.InstallationResponse toResponse(NotionInstallation i) {
        return new NotionDto.InstallationResponse(
                i.getId(), i.getProject().getId(),
                i.getIntegrationToken() != null,
                i.getDatabaseId(),
                i.getWorkspaceName(),
                i.getLastPolledAt(),
                i.getConnectedAt()
        );
    }

    private NotionDto.MappingResponse toMappingResponse(NotionUserMapping m) {
        return new NotionDto.MappingResponse(
                m.getId(), m.getUser().getId(),
                m.getUser().getName(),
                m.getNotionUserId(),
                m.getNotionUserName()
        );
    }
}
