package com.blackbox.domain.github.service;

import com.blackbox.domain.activity.entity.ActivityLog;
import com.blackbox.domain.activity.repository.ActivityLogRepository;
import com.blackbox.domain.github.entity.GitHubInstallation;
import com.blackbox.domain.github.repository.GitHubInstallationRepository;
import com.blackbox.domain.github.repository.GitHubUserMappingRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubWebhookService {

    private final GitHubInstallationRepository installationRepository;
    private final GitHubUserMappingRepository  mappingRepository;
    private final ActivityLogRepository        activityLogRepository;
    private final UserRepository               userRepository;

    /**
     * GitHub X-Hub-Signature-256 헤더 검증
     * 형식: "sha256=<hex>"
     */
    public boolean verifySignature(String repoFullName, String signatureHeader, byte[] body) {
        Optional<GitHubInstallation> inst = installationRepository.findByRepoFullName(repoFullName);
        if (inst.isEmpty() || inst.get().getWebhookSecret() == null) {
            log.warn("Webhook received for unknown or unconfigured repo: {}", repoFullName);
            return false;
        }
        String secret = inst.get().getWebhookSecret();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(body);
            String expected = "sha256=" + HexFormat.of().formatHex(digest);
            return constantTimeEquals(expected, signatureHeader);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    /** push 이벤트 처리 */
    @Transactional
    public void handlePush(String repoFullName, Map<String, Object> payload) {
        Optional<GitHubInstallation> instOpt = installationRepository.findByRepoFullName(repoFullName);
        if (instOpt.isEmpty()) return;
        GitHubInstallation inst = instOpt.get();
        Long projectId = inst.getProject().getId();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");
        if (commits == null) return;

        for (Map<String, Object> commit : commits) {
            String sha = (String) commit.get("id");
            if (sha == null) continue;

            if (activityLogRepository.existsByProjectIdAndSourceAndPayloadSha(
                    projectId, "GITHUB", sha)) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> author = (Map<String, Object>) commit.get("author");
            String email  = author != null ? (String) author.get("email") : null;
            String login  = author != null ? (String) author.get("username") : null;
            String message = (String) commit.get("message");

            Optional<User> user = resolveUser(projectId, login, email);
            if (user.isEmpty()) continue;

            @SuppressWarnings("unchecked")
            List<String> added    = (List<String>) commit.getOrDefault("added",    List.of());
            @SuppressWarnings("unchecked")
            List<String> removed  = (List<String>) commit.getOrDefault("removed",  List.of());
            @SuppressWarnings("unchecked")
            List<String> modified = (List<String>) commit.getOrDefault("modified", List.of());
            int filesChanged = added.size() + removed.size() + modified.size();

            Map<String, Object> logPayload = new HashMap<>();
            logPayload.put("sha",          sha);
            logPayload.put("message",      message != null ? message.lines().findFirst().orElse("") : "");
            logPayload.put("repo",         repoFullName);
            logPayload.put("filesChanged", filesChanged);
            logPayload.put("scoreWeight",  commitScoreWeight(filesChanged));

            activityLogRepository.save(ActivityLog.builder()
                    .project(inst.getProject())
                    .user(user.get())
                    .eventType(ActivityLog.EventType.GITHUB_PUSH)
                    .source("GITHUB")
                    .payload(logPayload)
                    .build());
            log.debug("Webhook push saved: sha={} repo={} files={}", sha, repoFullName, filesChanged);
        }
    }

    /** pull_request 이벤트 처리 */
    @Transactional
    public void handlePullRequest(String repoFullName, Map<String, Object> payload) {
        Optional<GitHubInstallation> instOpt = installationRepository.findByRepoFullName(repoFullName);
        if (instOpt.isEmpty()) return;
        GitHubInstallation inst = instOpt.get();
        Long projectId = inst.getProject().getId();

        String action = (String) payload.get("action");
        if (!"opened".equals(action) && !"closed".equals(action)) return;

        @SuppressWarnings("unchecked")
        Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
        if (pr == null) return;

        Object numberObj = pr.get("number");
        long number = numberObj instanceof Number n ? n.longValue() : 0L;
        String title = (String) pr.get("title");
        boolean merged = Boolean.TRUE.equals(pr.get("merged"));

        ActivityLog.EventType type = merged
                ? ActivityLog.EventType.GITHUB_PR_MERGED
                : ActivityLog.EventType.GITHUB_PR_OPENED;

        String key = "pr-" + number + "-" + type.name();
        if (activityLogRepository.existsByProjectIdAndSourceAndPayloadSha(
                projectId, "GITHUB", key)) return;

        @SuppressWarnings("unchecked")
        Map<String, Object> senderMap = (Map<String, Object>) payload.get("sender");
        String login = senderMap != null ? (String) senderMap.get("login") : null;

        Optional<User> user = resolveUser(projectId, login, null);
        if (user.isEmpty()) return;

        int changedFiles = pr.get("changed_files") instanceof Number n ? n.intValue() : 0;
        int additions    = pr.get("additions")     instanceof Number n ? n.intValue() : 0;
        int deletions    = pr.get("deletions")     instanceof Number n ? n.intValue() : 0;

        Map<String, Object> prPayload = new HashMap<>();
        prPayload.put("sha",          key);
        prPayload.put("pr",           number);
        prPayload.put("title",        title != null ? title : "");
        prPayload.put("repo",         repoFullName);
        prPayload.put("changedFiles", changedFiles);
        prPayload.put("additions",    additions);
        prPayload.put("deletions",    deletions);
        prPayload.put("scoreWeight",  prScoreWeight(changedFiles, additions + deletions));

        activityLogRepository.save(ActivityLog.builder()
                .project(inst.getProject())
                .user(user.get())
                .eventType(type)
                .source("GITHUB")
                .payload(prPayload)
                .build());
        log.debug("Webhook PR saved: {} repo={} files={}", key, repoFullName, changedFiles);
    }

    /* ── 유저 매핑 ── */
    private Optional<User> resolveUser(Long projectId, String login, String email) {
        if (login != null) {
            var mapping = mappingRepository.findByProjectIdAndGithubLogin(projectId, login);
            if (mapping.isPresent()) return Optional.of(mapping.get().getUser());
        }
        if (email != null) {
            return userRepository.findByEmail(email);
        }
        return Optional.empty();
    }

    /** 커밋 변경 파일 수 → scoreWeight */
    private double commitScoreWeight(int filesChanged) {
        if (filesChanged == 0)  return 0.5;
        if (filesChanged <= 5)  return 1.0;
        if (filesChanged <= 20) return 1.5;
        return 2.0;
    }

    /** PR 복잡도 → scoreWeight */
    private double prScoreWeight(int changedFiles, int linesChanged) {
        if (changedFiles > 20 || linesChanged > 300) return 2.5;
        if (changedFiles > 5  || linesChanged > 50)  return 1.5;
        return 1.0;
    }

    /** 타이밍 공격 방지를 위한 상수시간 비교 */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
