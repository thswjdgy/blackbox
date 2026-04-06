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

            activityLogRepository.save(ActivityLog.builder()
                    .project(inst.getProject())
                    .user(user.get())
                    .eventType(ActivityLog.EventType.GITHUB_PUSH)
                    .source("GITHUB")
                    .payload(Map.of(
                            "sha",     sha,
                            "message", message != null ? message.lines().findFirst().orElse("") : "",
                            "repo",    repoFullName
                    ))
                    .build());
            log.debug("Webhook push saved: sha={} repo={}", sha, repoFullName);
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

        activityLogRepository.save(ActivityLog.builder()
                .project(inst.getProject())
                .user(user.get())
                .eventType(type)
                .source("GITHUB")
                .payload(Map.of(
                        "sha",   key,
                        "pr",    number,
                        "title", title != null ? title : "",
                        "repo",  repoFullName
                ))
                .build());
        log.debug("Webhook PR saved: {} repo={}", key, repoFullName);
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
