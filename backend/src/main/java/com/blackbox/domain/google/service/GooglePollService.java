package com.blackbox.domain.google.service;

import com.blackbox.domain.activity.entity.ActivityLog;
import com.blackbox.domain.activity.repository.ActivityLogRepository;
import com.blackbox.domain.google.dto.GoogleDto;
import com.blackbox.domain.google.entity.GoogleInstallation;
import com.blackbox.domain.google.repository.GoogleInstallationRepository;
import com.blackbox.domain.google.repository.GoogleUserMappingRepository;
import com.blackbox.domain.project.repository.ProjectMemberRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GooglePollService {

    private static final String DRIVE_API  = "https://www.googleapis.com/drive/v3";
    private static final String FORMS_API  = "https://forms.googleapis.com/v1";

    private static final DateTimeFormatter DRIVE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ISO_INSTANT;

    private final GoogleInstallationRepository installationRepository;
    private final GoogleUserMappingRepository  mappingRepository;
    private final GoogleOAuthService           oauthService;
    private final ActivityLogRepository        activityLogRepository;
    private final ProjectMemberRepository      projectMemberRepository;
    private final UserRepository               userRepository;
    private final RestTemplate                 restTemplate;

    /** 30분마다 모든 프로젝트 폴링 */
    @Scheduled(fixedDelay = 1_800_000)
    public void pollAll() {
        List<GoogleInstallation> list = installationRepository.findAllByRefreshTokenIsNotNull();
        log.info("Google polling: {} projects", list.size());
        list.forEach(inst -> {
            try { poll(inst); }
            catch (Exception e) {
                log.warn("Google poll failed [project={}]: {}", inst.getProject().getId(), e.getMessage());
            }
        });
    }

    /** 수동 즉시 폴링 — 타임스탬프 리셋 후 최대 7일치 재조회 */
    @Transactional
    public GoogleDto.PollResult pollNow(Long projectId) {
        GoogleInstallation inst = installationRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalStateException("Google 연동 없음"));
        inst.setDrivePolledAt(null);
        inst.setSheetsPolledAt(null);
        inst.setFormsPolledAt(null);
        return poll(inst);
    }

    @Transactional
    protected GoogleDto.PollResult poll(GoogleInstallation inst) {
        String accessToken = oauthService.getValidAccessToken(inst);
        if (accessToken == null) {
            log.warn("Google access token 없음 [project={}]", inst.getProject().getId());
            return new GoogleDto.PollResult(0, 0, 0, 0);
        }

        Long projectId = inst.getProject().getId();
        Instant now = Instant.now();

        int drive  = pollDrive(inst, projectId, accessToken, now);
        int sheets = pollSheets(inst, projectId, accessToken, now);
        int forms  = pollForms(inst, projectId, accessToken, now);

        inst.setDrivePolledAt(now);
        inst.setSheetsPolledAt(now);
        inst.setFormsPolledAt(now);
        installationRepository.save(inst);

        log.info("Google poll [project={}]: drive={}, sheets={}, forms={}", projectId, drive, sheets, forms);
        return new GoogleDto.PollResult(drive, sheets, forms, drive + sheets + forms);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Drive 폴링
    // ──────────────────────────────────────────────────────────────────────
    private int pollDrive(GoogleInstallation inst, Long projectId, String token, Instant now) {
        Instant since = inst.getDrivePolledAt() != null
                ? inst.getDrivePolledAt()
                : now.minusSeconds(60L * 60 * 24 * 7);

        String sinceStr = DRIVE_FMT.format(since);
        StringBuilder q = new StringBuilder("modifiedTime > '" + sinceStr + "'");
        q.append(" and mimeType != 'application/vnd.google-apps.folder'");
        q.append(" and 'me' in owners");   // 내가 소유한 파일만
        if (inst.getDriveFolderId() != null && !inst.getDriveFolderId().isBlank()) {
            q.append(" and '" + inst.getDriveFolderId() + "' in parents");
        }

        int count = 0;
        String pageToken = null;

        do {
            UriComponentsBuilder ub = UriComponentsBuilder.fromUriString(DRIVE_API + "/files")
                    .queryParam("q", q.toString())
                    .queryParam("fields", "nextPageToken,files(id,name,mimeType,createdTime,modifiedTime,owners(emailAddress),webViewLink)")
                    .queryParam("orderBy", "modifiedTime desc")
                    .queryParam("pageSize", "100")
                    .queryParam("spaces", "drive");
            if (pageToken != null) ub.queryParam("pageToken", pageToken);

            Map<String, Object> resp = get(ub.build().toUriString(), token);
            if (resp == null) break;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> files = (List<Map<String, Object>>) resp.get("files");
            if (files == null || files.isEmpty()) break;

            for (Map<String, Object> file : files) {
                if (processDriveFile(file, inst, projectId)) count++;
            }
            pageToken = (String) resp.get("nextPageToken");
        } while (pageToken != null);

        return count;
    }

    @SuppressWarnings("unchecked")
    private boolean processDriveFile(Map<String, Object> file, GoogleInstallation inst, Long projectId) {
        String fileId      = (String) file.get("id");
        String name        = (String) file.get("name");
        String createdTime = (String) file.get("createdTime");
        String modifiedTime= (String) file.get("modifiedTime");
        String webViewLink = (String) file.get("webViewLink");
        if (fileId == null || modifiedTime == null) return false;

        // 중복 방지
        String dedupKey = "gdrive-" + fileId + "-" + modifiedTime;
        if (activityLogRepository.existsByProjectIdAndSourceAndPayloadSha(
                projectId, "GOOGLE_DRIVE", dedupKey)) return false;

        // 신규 업로드 vs 수정 판단
        boolean isNew = createdTime != null &&
                Math.abs(Instant.parse(createdTime).toEpochMilli() -
                         Instant.parse(modifiedTime).toEpochMilli()) < 60_000;
        ActivityLog.EventType type = isNew
                ? ActivityLog.EventType.GDRIVE_FILE_UPLOADED
                : ActivityLog.EventType.GDRIVE_FILE_MODIFIED;

        // 소유자로 유저 해석
        String ownerEmail = null;
        List<Map<String, Object>> owners = (List<Map<String, Object>>) file.get("owners");
        if (owners != null && !owners.isEmpty()) {
            ownerEmail = (String) owners.get(0).get("emailAddress");
        }
        User user = resolveUser(projectId, ownerEmail);
        if (user == null) return false;

        activityLogRepository.save(ActivityLog.builder()
                .project(inst.getProject())
                .user(user)
                .eventType(type)
                .source("GOOGLE_DRIVE")
                .payload(Map.of(
                        "sha",         dedupKey,
                        "fileId",      fileId,
                        "name",        name != null ? name : "",
                        "url",         webViewLink != null ? webViewLink : ""
                ))
                .build());
        return true;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Sheets 폴링 (Drive Revisions API 활용)
    // ──────────────────────────────────────────────────────────────────────
    private int pollSheets(GoogleInstallation inst, Long projectId, String token, Instant now) {
        if (inst.getSheetId() == null || inst.getSheetId().isBlank()) return 0;

        Instant since = inst.getSheetsPolledAt() != null
                ? inst.getSheetsPolledAt()
                : now.minusSeconds(60L * 60 * 24 * 7);

        String url = DRIVE_API + "/files/" + inst.getSheetId() + "/revisions"
                + "?fields=revisions(id,modifiedTime,lastModifyingUser(emailAddress,displayName))";

        Map<String, Object> resp = get(url, token);
        if (resp == null) return 0;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> revisions = (List<Map<String, Object>>) resp.get("revisions");
        if (revisions == null) return 0;

        int count = 0;
        for (Map<String, Object> rev : revisions) {
            String revId       = (String) rev.get("id");
            String modifiedTime= (String) rev.get("modifiedTime");
            if (revId == null || modifiedTime == null) continue;
            if (Instant.parse(modifiedTime).isBefore(since)) continue;

            String dedupKey = "gsheet-" + inst.getSheetId() + "-rev-" + revId;
            if (activityLogRepository.existsByProjectIdAndSourceAndPayloadSha(
                    projectId, "GOOGLE_DRIVE", dedupKey)) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> modifier = (Map<String, Object>) rev.get("lastModifyingUser");
            String email = modifier != null ? (String) modifier.get("emailAddress") : null;
            String displayName = modifier != null ? (String) modifier.get("displayName") : null;

            User user = resolveUser(projectId, email);
            if (user == null) continue;

            activityLogRepository.save(ActivityLog.builder()
                    .project(inst.getProject())
                    .user(user)
                    .eventType(ActivityLog.EventType.GSHEET_EDITED)
                    .source("GOOGLE_DRIVE")
                    .payload(Map.of(
                            "sha",       dedupKey,
                            "sheetId",   inst.getSheetId(),
                            "revisionId",revId,
                            "editor",    displayName != null ? displayName : email != null ? email : ""
                    ))
                    .build());
            count++;
        }
        return count;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Forms 폴링
    // ──────────────────────────────────────────────────────────────────────
    private int pollForms(GoogleInstallation inst, Long projectId, String token, Instant now) {
        if (inst.getFormId() == null || inst.getFormId().isBlank()) return 0;

        Instant since = inst.getFormsPolledAt() != null
                ? inst.getFormsPolledAt()
                : now.minusSeconds(60L * 60 * 24 * 7);

        String sinceIso = ISO_FMT.format(since);
        String url = UriComponentsBuilder.fromUriString(FORMS_API + "/forms/" + inst.getFormId() + "/responses")
                .queryParam("filter", "timestamp >= " + sinceIso)
                .build().toUriString();

        Map<String, Object> resp = get(url, token);
        if (resp == null) return 0;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> responses = (List<Map<String, Object>>) resp.get("responses");
        if (responses == null) return 0;

        int count = 0;
        for (Map<String, Object> formResp : responses) {
            String responseId  = (String) formResp.get("responseId");
            String submittedAt = (String) formResp.get("lastSubmittedTime");
            String email       = (String) formResp.get("respondentEmail");
            if (responseId == null) continue;

            String dedupKey = "gform-" + inst.getFormId() + "-resp-" + responseId;
            if (activityLogRepository.existsByProjectIdAndSourceAndPayloadSha(
                    projectId, "GOOGLE_DRIVE", dedupKey)) continue;

            User user = resolveUser(projectId, email);
            if (user == null) continue;

            activityLogRepository.save(ActivityLog.builder()
                    .project(inst.getProject())
                    .user(user)
                    .eventType(ActivityLog.EventType.GFORM_RESPONSE_SUBMITTED)
                    .source("GOOGLE_DRIVE")
                    .payload(Map.of(
                            "sha",        dedupKey,
                            "formId",     inst.getFormId(),
                            "responseId", responseId,
                            "submittedAt",submittedAt != null ? submittedAt : ""
                    ))
                    .build());
            count++;
        }
        return count;
    }

    // ──────────────────────────────────────────────────────────────────────
    // 유저 해석
    // ──────────────────────────────────────────────────────────────────────
    private User resolveUser(Long projectId, String email) {
        // 1. Google email 매핑
        if (email != null) {
            var mapping = mappingRepository.findByProjectIdAndGoogleEmail(projectId, email);
            if (mapping.isPresent()) return mapping.get().getUser();
            // 2. 이메일로 직접 조회
            var byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) return byEmail.get();
        }
        // 3. 프로젝트 첫 번째 멤버로 fallback
        return projectMemberRepository.findByProjectId(projectId).stream()
                .findFirst().map(m -> m.getUser()).orElse(null);
    }

    // ──────────────────────────────────────────────────────────────────────
    // HTTP GET
    // ──────────────────────────────────────────────────────────────────────
    private Map<String, Object> get(String url, String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {});
            return res.getBody();
        } catch (Exception e) {
            log.warn("Google API error [{}]: {}", url, e.getMessage());
            return null;
        }
    }
}
