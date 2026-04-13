package com.blackbox.domain.notion.service;

import com.blackbox.domain.meeting.entity.Meeting;
import com.blackbox.domain.meeting.repository.MeetingRepository;
import com.blackbox.domain.notion.entity.NotionInstallation;
import com.blackbox.domain.notion.repository.NotionInstallationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotionPushService {

    private static final String NOTION_API     = "https://api.notion.com/v1";
    private static final String NOTION_VERSION = "2022-06-28";

    private final NotionInstallationRepository installationRepository;
    private final MeetingRepository            meetingRepository;
    private final RestTemplate                 restTemplate;

    @Value("${notion.token:}")
    private String globalToken;

    @Value("${notion.database-id:}")
    private String globalDatabaseId;

    /** 회의록 → Notion 페이지 생성. 생성된 페이지 URL 반환 */
    public String pushMeeting(Long projectId, Long meetingId) {
        NotionInstallation inst = installationRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalStateException("Notion 연동 없음"));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalStateException("회의를 찾을 수 없습니다."));

        String token      = resolveToken(inst);
        String databaseId = resolveDatabaseId(inst);

        if (token == null || token.isBlank())
            throw new IllegalStateException("Notion 토큰이 설정되지 않았습니다.");
        if (databaseId == null || databaseId.isBlank())
            throw new IllegalStateException("Notion Database ID가 설정되지 않았습니다. 설정 → Notion에서 Database ID를 입력해 주세요.");

        String titleKey = fetchTitlePropertyKey(databaseId, token);
        Map<String, Object> body = buildMeetingPage(meeting, databaseId, titleKey);

        Map<String, Object> result = post(NOTION_API + "/pages", token, body);
        if (result == null) throw new IllegalStateException("Notion 페이지 생성에 실패했습니다. 토큰/Database ID를 확인해 주세요.");

        String pageId = (String) result.get("id");
        return "https://notion.so/" + pageId.replace("-", "");
    }

    /* ── DB 스키마에서 title 타입 속성 키 조회 ── */
    private String fetchTitlePropertyKey(String databaseId, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Notion-Version", NOTION_VERSION);

            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                    NOTION_API + "/databases/" + databaseId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {});

            Map<String, Object> body = res.getBody();
            if (body != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> props = (Map<String, Object>) body.get("properties");
                if (props != null) {
                    for (Map.Entry<String, Object> e : props.entrySet()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> p = (Map<String, Object>) e.getValue();
                        if ("title".equals(p.get("type"))) return e.getKey();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Notion DB schema 조회 실패: {}", e.getMessage());
        }
        return "이름"; // 한국어 Notion DB 기본 컬럼명
    }

    /* ── 회의 데이터 → Notion page body ── */
    private Map<String, Object> buildMeetingPage(Meeting meeting, String databaseId, String titleKey) {
        List<Map<String, Object>> children = new ArrayList<>();

        // 날짜 정보
        String dateStr = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                .withZone(ZoneId.of("Asia/Seoul"))
                .format(meeting.getMeetingAt());
        children.add(paragraph("📅 " + dateStr));

        if (meeting.getPurpose() != null && !meeting.getPurpose().isBlank()) {
            children.add(heading2("목적"));
            children.add(paragraph(meeting.getPurpose()));
        }

        if (meeting.getNotes() != null && !meeting.getNotes().isBlank()) {
            children.add(heading2("회의 노트"));
            for (String line : meeting.getNotes().split("\n")) {
                children.add(paragraph(line.isBlank() ? " " : line));
            }
        }

        if (meeting.getDecisions() != null && !meeting.getDecisions().isBlank()) {
            children.add(heading2("결정 사항"));
            for (String line : meeting.getDecisions().split("\n")) {
                children.add(paragraph(line.isBlank() ? " " : line));
            }
        }

        // 출석 인원
        if (!meeting.getAttendees().isEmpty()) {
            children.add(heading2("출석"));
            String attendees = meeting.getAttendees().stream()
                    .map(a -> a.getUser().getName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            children.add(paragraph(attendees));
        }

        children.add(paragraph("─────────────────────"));
        children.add(paragraph("Blackbox 앱에서 내보낸 회의록입니다."));

        return Map.of(
                "parent",     Map.of("database_id", databaseId),
                "properties", Map.of(
                        titleKey, Map.of(
                                "title", List.of(Map.of(
                                        "type", "text",
                                        "text", Map.of("content", meeting.getTitle())
                                ))
                        )
                ),
                "children",   children
        );
    }

    private Map<String, Object> heading2(String content) {
        return Map.of(
                "object", "block",
                "type",   "heading_2",
                "heading_2", Map.of("rich_text", List.of(
                        Map.of("type", "text", "text", Map.of("content", content))
                ))
        );
    }

    private Map<String, Object> paragraph(String content) {
        return Map.of(
                "object", "block",
                "type",   "paragraph",
                "paragraph", Map.of("rich_text", List.of(
                        Map.of("type", "text", "text", Map.of("content", content))
                ))
        );
    }

    private String resolveToken(NotionInstallation inst) {
        String t = inst.getIntegrationToken();
        return (t != null && !t.isBlank()) ? t : globalToken;
    }

    private String resolveDatabaseId(NotionInstallation inst) {
        String d = inst.getDatabaseId();
        return (d != null && !d.isBlank()) ? d : globalDatabaseId;
    }

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
            log.warn("Notion push error [{}]: {}", url, e.getMessage());
            return null;
        }
    }
}
