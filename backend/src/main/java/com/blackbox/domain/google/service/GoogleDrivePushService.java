package com.blackbox.domain.google.service;

import com.blackbox.domain.google.entity.GoogleInstallation;
import com.blackbox.domain.google.repository.GoogleInstallationRepository;
import com.blackbox.domain.meeting.entity.Meeting;
import com.blackbox.domain.meeting.repository.MeetingRepository;
import com.blackbox.domain.vault.entity.FileVault;
import com.blackbox.domain.vault.repository.FileVaultRepository;
import com.blackbox.global.exception.BusinessException;
import com.blackbox.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDrivePushService {

    private static final String DRIVE_UPLOAD_URL =
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    private final GoogleInstallationRepository installationRepository;
    private final GoogleOAuthService           oauthService;
    private final MeetingRepository            meetingRepository;
    private final FileVaultRepository          fileVaultRepository;
    private final RestTemplate                 restTemplate;

    @Value("${file.upload-dir:/data/uploads}")
    private String uploadDir;

    // ──────────────────────────────────────────────────────────────────────
    // 회의록 → Google Drive (Google Docs)
    // ──────────────────────────────────────────────────────────────────────
    public String pushMeeting(Long projectId, Long meetingId) {
        GoogleInstallation inst = getInstallation(projectId);
        String token = oauthService.getValidAccessToken(inst);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        String content = buildMeetingHtml(meeting);
        String docName  = "[회의록] " + meeting.getTitle() + " (" + DATE_FMT.format(meeting.getMeetingAt()) + ")";

        // mimeType=application/vnd.google-apps.document → Drive가 Google Docs로 변환
        String fileId = uploadMultipart(token, docName,
                "application/vnd.google-apps.document",
                content.getBytes(StandardCharsets.UTF_8),
                "text/html; charset=UTF-8",
                inst.getDriveFolderId());

        String url = "https://docs.google.com/document/d/" + fileId + "/edit";
        log.info("Meeting {} pushed to Drive: {}", meetingId, url);
        return url;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Vault 파일 → Google Drive
    // ──────────────────────────────────────────────────────────────────────
    public String pushVaultFile(Long projectId, Long fileId) {
        GoogleInstallation inst = getInstallation(projectId);
        String token = oauthService.getValidAccessToken(inst);

        FileVault vault = fileVaultRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VAULT_FILE_NOT_FOUND));

        Path filePath = Path.of(uploadDir).resolve(vault.getFilePath());
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패: " + vault.getFileName(), e);
        }

        String mime = vault.getMimeType() != null ? vault.getMimeType() : "application/octet-stream";
        String driveFileId = uploadMultipart(token, vault.getFileName(), null,
                bytes, mime, inst.getDriveFolderId());

        String url = "https://drive.google.com/file/d/" + driveFileId + "/view";
        log.info("VaultFile {} pushed to Drive: {}", fileId, url);
        return url;
    }

    // ──────────────────────────────────────────────────────────────────────
    // 공통 multipart/related 업로드
    // ──────────────────────────────────────────────────────────────────────
    private String uploadMultipart(String accessToken, String name,
                                   String targetMimeType,   // Google Docs 변환용 (null이면 원본 유지)
                                   byte[] content, String contentMime,
                                   String folderId) {
        String boundary = "bb_" + System.currentTimeMillis();

        // metadata JSON
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("name", name);
        if (targetMimeType != null) meta.put("mimeType", targetMimeType);
        if (folderId != null && !folderId.isBlank()) meta.put("parents", new String[]{folderId});
        String metaJson = toJson(meta);

        // multipart/related 바디 수동 조립
        byte[] metaBytes  = metaJson.getBytes(StandardCharsets.UTF_8);
        String sep        = "\r\n";
        String dashes     = "--" + boundary;

        StringBuilder header = new StringBuilder();
        header.append(dashes).append(sep);
        header.append("Content-Type: application/json; charset=UTF-8").append(sep).append(sep);

        StringBuilder middle = new StringBuilder();
        middle.append(sep).append(dashes).append(sep);
        middle.append("Content-Type: ").append(contentMime).append(sep).append(sep);

        String footer = sep + dashes + "--";

        byte[] headerB  = header.toString().getBytes(StandardCharsets.UTF_8);
        byte[] metaB    = metaBytes;
        byte[] middleB  = middle.toString().getBytes(StandardCharsets.UTF_8);
        byte[] contentB = content;
        byte[] footerB  = footer.getBytes(StandardCharsets.UTF_8);

        byte[] body = concat(headerB, metaB, middleB, contentB, footerB);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "multipart/related; boundary=" + boundary));
        headers.setContentLength(body.length);
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                DRIVE_UPLOAD_URL,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<>() {});

        Map<String, Object> responseBody = res.getBody();
        if (responseBody == null || !responseBody.containsKey("id")) {
            throw new RuntimeException("Drive 업로드 응답 오류");
        }
        return (String) responseBody.get("id");
    }

    // ──────────────────────────────────────────────────────────────────────
    // 회의록 HTML 생성
    // ──────────────────────────────────────────────────────────────────────
    private String buildMeetingHtml(Meeting meeting) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>").append(esc(meeting.getTitle())).append("</h1>");
        sb.append("<p><b>일시:</b> ").append(DATE_FMT.format(meeting.getMeetingAt())).append("</p>");

        if (meeting.getPurpose() != null && !meeting.getPurpose().isBlank()) {
            sb.append("<h2>목적</h2><p>").append(esc(meeting.getPurpose())).append("</p>");
        }
        if (meeting.getNotes() != null && !meeting.getNotes().isBlank()) {
            sb.append("<h2>회의 내용</h2><p>")
              .append(esc(meeting.getNotes()).replace("\n", "<br>"))
              .append("</p>");
        }
        if (meeting.getDecisions() != null && !meeting.getDecisions().isBlank()) {
            sb.append("<h2>결정사항</h2><p>")
              .append(esc(meeting.getDecisions()).replace("\n", "<br>"))
              .append("</p>");
        }
        if (!meeting.getAttendees().isEmpty()) {
            sb.append("<h2>참석자</h2><ul>");
            meeting.getAttendees().forEach(a ->
                sb.append("<li>").append(esc(a.getUser().getName())).append("</li>"));
            sb.append("</ul>");
        }
        sb.append("<hr><p><i>Blackbox에서 내보낸 회의록</i></p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────────────
    // 유틸
    // ──────────────────────────────────────────────────────────────────────
    private GoogleInstallation getInstallation(Long projectId) {
        return installationRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTEGRATION_NOT_FOUND));
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        map.forEach((k, v) -> {
            sb.append("\"").append(k).append("\":");
            if (v instanceof String) sb.append("\"").append(v).append("\"");
            else if (v instanceof String[]) {
                sb.append("[");
                for (String s : (String[]) v) sb.append("\"").append(s).append("\",");
                if (((String[]) v).length > 0) sb.setLength(sb.length() - 1);
                sb.append("]");
            } else sb.append(v);
            sb.append(",");
        });
        if (sb.length() > 1) sb.setLength(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    private static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays) total += a.length;
        byte[] result = new byte[total];
        int pos = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, pos, a.length);
            pos += a.length;
        }
        return result;
    }
}
