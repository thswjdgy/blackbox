package com.blackbox.domain.notion.service;

import com.blackbox.domain.activity.entity.ActivityLog;
import com.blackbox.domain.activity.repository.ActivityLogRepository;
import com.blackbox.domain.notion.dto.NotionDto;
import com.blackbox.domain.notion.entity.NotionInstallation;
import com.blackbox.domain.notion.repository.NotionInstallationRepository;
import com.blackbox.domain.notion.repository.NotionUserMappingRepository;
import com.blackbox.domain.project.repository.ProjectMemberRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotionPollService {

    private static final String NOTION_API     = "https://api.notion.com/v1";
    private static final String NOTION_VERSION = "2022-06-28";

    private final NotionInstallationRepository installationRepository;
    private final NotionUserMappingRepository  mappingRepository;
    private final ActivityLogRepository        activityLogRepository;
    private final UserRepository               userRepository;
    private final ProjectMemberRepository      projectMemberRepository;
    private final RestTemplate                 restTemplate;

    /** .env / docker-compose 환경변수로 주입되는 글로벌 토큰 */
    @Value("${notion.token:}")
    private String globalToken;

    @Value("${notion.database-id:}")
    private String globalDatabaseId;

    /** 30분마다 연동된 모든 프로젝트 폴링 */
    @Scheduled(fixedDelay = 1_800_000)
    public void pollAll() {
        List<NotionInstallation> installations = installationRepository.findAllByIntegrationTokenIsNotNull();
        log.info("Notion polling: {} projects", installations.size());
        installations.forEach(inst -> {
            try { poll(inst); }
            catch (Exception e) { log.warn("Notion poll failed for project {}: {}", inst.getProject().getId(), e.getMessage()); }
        });
    }

    /** 수동 즉시 폴링 — lastPolledAt을 리셋해서 항상 최대 7일치 재조회 */
    @Transactional
    public NotionDto.PollResult pollNow(Long projectId) {
        NotionInstallation inst = installationRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalStateException("Notion 연동 없음"));
        inst.setLastPolledAt(null);  // 강제 리셋 → 7일치 재조회
        return poll(inst);
    }

    @Transactional
    protected NotionDto.PollResult poll(NotionInstallation inst) {
        // DB에 토큰 없으면 글로벌 env 토큰 사용
        String token = (inst.getIntegrationToken() != null && !inst.getIntegrationToken().isBlank())
                ? inst.getIntegrationToken()
                : globalToken;

        if (token == null || token.isBlank()) {
            log.warn("Notion token 없음 — project={}", inst.getProject().getId());
            return new NotionDto.PollResult(0, 0, 0);
        }

        // DB에 Database ID 없으면 글로벌 env 값 사용
        String databaseId = (inst.getDatabaseId() != null && !inst.getDatabaseId().isBlank())
                ? inst.getDatabaseId()
                : (globalDatabaseId != null && !globalDatabaseId.isBlank() ? globalDatabaseId : null);

        // 임시로 token을 inst에 주입해서 하위 메서드에서 재사용
        inst.setIntegrationToken(token);
        if (databaseId != null) inst.setDatabaseId(databaseId);

        Instant since = inst.getLastPolledAt() != null
                ? inst.getLastPolledAt()
                : Instant.now().minusSeconds(60 * 60 * 24 * 7); // 최초: 7일치

        Long projectId = inst.getProject().getId();
        int created = 0, edited = 0;

        if (inst.getDatabaseId() != null && !inst.getDatabaseId().isBlank()) {
            // 특정 Database 폴링
            var result = fetchDatabase(inst, projectId, since);
            created += result[0]; edited += result[1];
        } else {
            // 워크스페이스 전체 검색
            var result = fetchSearch(inst, projectId, since);
            created += result[0]; edited += result[1];
        }

        inst.setLastPolledAt(Instant.now());
        installationRepository.save(inst);

        log.info("Notion poll [project={}]: {} created, {} edited", projectId, created, edited);
        return new NotionDto.PollResult(created, edited, created + edited);
    }

    /* ── 워크스페이스 검색 (database_id 없을 때) ── */
    private int[] fetchSearch(NotionInstallation inst, Long projectId, Instant since) {
        Map<String, Object> body = Map.of(
                "sort",   Map.of("direction", "descending", "timestamp", "last_edited_time"),
                "filter", Map.of("property", "object", "value", "page"),
                "page_size", 100
        );

        int created = 0, edited = 0;
        String cursor = null;
        boolean hasMore = true;

        while (hasMore) {
            Map<String, Object> req;
            if (cursor != null) {
                req = new LinkedHashMap<>(body);
                req.put("start_cursor", cursor);
            } else {
                req = body;
            }

            Map<String, Object> resp = post(NOTION_API + "/search", inst.getIntegrationToken(), req);
            if (resp == null) break;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
            if (results == null || results.isEmpty()) break;

            boolean stop = false;
            for (Map<String, Object> page : results) {
                String lastEdited = (String) page.get("last_edited_time");
                if (lastEdited == null) continue;

                Instant editedAt = Instant.parse(lastEdited);
                if (editedAt.isBefore(since)) { stop = true; break; }

                int[] r = processPage(inst, projectId, page, since);
                created += r[0]; edited += r[1];
            }

            if (stop) break;
            hasMore = Boolean.TRUE.equals(resp.get("has_more"));
            cursor  = (String) resp.get("next_cursor");
        }

        return new int[]{created, edited};
    }

    /* ── 특정 Database 폴링 ── */
    private int[] fetchDatabase(NotionInstallation inst, Long projectId, Instant since) {
        Map<String, Object> body = Map.of(
                "sorts",     List.of(Map.of("timestamp", "last_edited_time", "direction", "descending")),
                "page_size", 100
        );

        String url = NOTION_API + "/databases/" + inst.getDatabaseId() + "/query";
        int created = 0, edited = 0;
        String cursor = null;
        boolean hasMore = true;

        while (hasMore) {
            Map<String, Object> req;
            if (cursor != null) {
                req = new LinkedHashMap<>(body);
                req.put("start_cursor", cursor);
            } else {
                req = body;
            }

            Map<String, Object> resp = post(url, inst.getIntegrationToken(), req);
            if (resp == null) break;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
            if (results == null || results.isEmpty()) break;

            boolean stop = false;
            for (Map<String, Object> page : results) {
                String lastEdited = (String) page.get("last_edited_time");
                if (lastEdited != null && Instant.parse(lastEdited).isBefore(since)) {
                    stop = true;
                    break;
                }
                int[] r = processPage(inst, projectId, page, since);
                created += r[0]; edited += r[1];
            }

            if (stop) break;
            hasMore = Boolean.TRUE.equals(resp.get("has_more"));
            cursor  = (String) resp.get("next_cursor");
        }

        return new int[]{created, edited};
    }

    /* ── 페이지 1건 처리 → activity_log 저장 ── */
    private int[] processPage(NotionInstallation inst, Long projectId,
                              Map<String, Object> page, Instant since) {
        String pageId      = (String) page.get("id");
        String createdTime = (String) page.get("created_time");
        String lastEdited  = (String) page.get("last_edited_time");
        if (pageId == null || lastEdited == null) return new int[]{0, 0};

        Instant editedAt  = Instant.parse(lastEdited);
        Instant createdAt = createdTime != null ? Instant.parse(createdTime) : editedAt;
        boolean isNew     = createdAt.isAfter(since);

        ActivityLog.EventType type = isNew
                ? ActivityLog.EventType.NOTION_PAGE_CREATED
                : ActivityLog.EventType.NOTION_PAGE_EDITED;

        // 중복 방지 키: pageId + lastEditedTime
        String dedupKey = "notion-" + pageId + "-" + lastEdited;
        if (activityLogRepository.existsByProjectIdAndSourceAndPayloadSha(
                projectId, "NOTION", dedupKey)) return new int[]{0, 0};

        // 유저 확인
        @SuppressWarnings("unchecked")
        Map<String, Object> editorMap = (Map<String, Object>) page.get("last_edited_by");
        Optional<User> user = resolveUser(projectId, editorMap, inst.getIntegrationToken());
        if (user.isEmpty()) {
            // 유저 매핑 실패 시 프로젝트 첫 번째 멤버로 fallback
            user = projectMemberRepository.findByProjectId(projectId)
                    .stream().findFirst().map(m -> m.getUser());
            if (user.isEmpty()) return new int[]{0, 0};
        }

        // 페이지 제목 추출
        String title = extractTitle(page);

        activityLogRepository.save(ActivityLog.builder()
                .project(inst.getProject())
                .user(user.get())
                .eventType(type)
                .source("NOTION")
                .payload(Map.of(
                        "sha",     dedupKey,
                        "pageId",  pageId,
                        "title",   title,
                        "url",     "https://notion.so/" + pageId.replace("-", "")
                ))
                .build());

        return isNew ? new int[]{1, 0} : new int[]{0, 1};
    }

    /* ── 유저 해석 ── */
    private Optional<User> resolveUser(Long projectId, Map<String, Object> editorMap, String token) {
        if (editorMap == null) return Optional.empty();
        String notionUserId = (String) editorMap.get("id");

        // 1. 수동 매핑
        if (notionUserId != null) {
            var mapping = mappingRepository.findByProjectIdAndNotionUserId(projectId, notionUserId);
            if (mapping.isPresent()) return Optional.of(mapping.get().getUser());
        }

        // 2. page 응답의 person.email (있는 경우)
        @SuppressWarnings("unchecked")
        Map<String, Object> person = (Map<String, Object>) editorMap.get("person");
        if (person != null) {
            String email = (String) person.get("email");
            if (email != null) return userRepository.findByEmail(email);
        }

        // 3. Notion Users API로 이메일 조회 후 자동 매핑
        if (notionUserId != null && token != null) {
            String email = fetchNotionUserEmail(notionUserId, token);
            if (email != null) return userRepository.findByEmail(email);
        }

        return Optional.empty();
    }

    /* ── Notion Users API로 이메일 조회 ── */
    private String fetchNotionUserEmail(String notionUserId, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Notion-Version", NOTION_VERSION);

            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                    NOTION_API + "/users/" + notionUserId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {});

            Map<String, Object> body = res.getBody();
            if (body == null) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> person = (Map<String, Object>) body.get("person");
            return person != null ? (String) person.get("email") : null;
        } catch (Exception e) {
            log.debug("Notion user email 조회 실패 [{}]: {}", notionUserId, e.getMessage());
            return null;
        }
    }

    /* ── 페이지 제목 추출 ── */
    @SuppressWarnings("unchecked")
    private String extractTitle(Map<String, Object> page) {
        try {
            Map<String, Object> props = (Map<String, Object>) page.get("properties");
            if (props == null) return "Untitled";

            // "title", "Name", "이름" 순으로 탐색
            for (String key : new String[]{"title", "Name", "이름", "제목"}) {
                Map<String, Object> prop = (Map<String, Object>) props.get(key);
                if (prop == null) continue;
                List<Map<String, Object>> titleArr = (List<Map<String, Object>>) prop.get("title");
                if (titleArr != null && !titleArr.isEmpty()) {
                    String plain = (String) titleArr.get(0).get("plain_text");
                    if (plain != null && !plain.isBlank()) return plain;
                }
            }
        } catch (Exception ignored) {}
        return "Untitled";
    }

    /* ── HTTP POST ── */
    private Map<String, Object> post(String url, String token, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Notion-Version", NOTION_VERSION);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    new ParameterizedTypeReference<>() {});
            return res.getBody();
        } catch (Exception e) {
            log.warn("Notion API error [{}]: {}", url, e.getMessage());
            return null;
        }
    }
}
