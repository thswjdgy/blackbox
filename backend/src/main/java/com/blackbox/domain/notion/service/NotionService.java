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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotionService {

    private static final String NOTION_API     = "https://api.notion.com/v1";
    private static final String NOTION_VERSION = "2022-06-28";

    private final NotionInstallationRepository installationRepository;
    private final NotionUserMappingRepository  mappingRepository;
    private final ProjectRepository            projectRepository;
    private final UserRepository               userRepository;
    private final RestTemplate                 restTemplate;

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
            throw new BusinessException(ErrorCode.ALREADY_MAPPED);

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

    /** Notion 워크스페이스 사용자 목록 조회 */
    public List<NotionDto.NotionUser> listUsers(Long projectId) {
        NotionInstallation inst = installationRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "Notion 연동이 필요합니다."));
        if (inst.getIntegrationToken() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Integration Token이 없습니다.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(inst.getIntegrationToken());
        headers.set("Notion-Version", NOTION_VERSION);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                NOTION_API + "/users",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>)
                (res.getBody() != null ? res.getBody().get("results") : List.of());

        return (results == null ? List.<Map<String,Object>>of() : results).stream()
                .filter(u -> "person".equals(u.get("type")))
                .map(u -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> person = (Map<String, Object>) u.getOrDefault("person", Map.of());
                    String email = (String) person.get("email");
                    return new NotionDto.NotionUser(
                            (String) u.get("id"),
                            (String) u.getOrDefault("name", ""),
                            email
                    );
                })
                .toList();
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
