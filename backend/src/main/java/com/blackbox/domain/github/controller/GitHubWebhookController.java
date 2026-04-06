package com.blackbox.domain.github.controller;

import com.blackbox.domain.github.service.GitHubWebhookService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/github/webhook")
@RequiredArgsConstructor
public class GitHubWebhookController {

    private final GitHubWebhookService webhookService;
    private final ObjectMapper         objectMapper;

    /**
     * GitHub App / Repository Webhook 수신
     * Content-Type: application/json
     * Headers: X-GitHub-Event, X-Hub-Signature-256, X-GitHub-Hook-Installation-Target-ID
     */
    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "")          String event,
            @RequestHeader(value = "X-Hub-Signature-256", defaultValue = "")     String sig,
            @RequestHeader(value = "X-GitHub-Hook-Installation-Target-ID", required = false) String targetId,
            @RequestBody byte[] rawBody) {

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawBody, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Webhook body parse error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        // repo 식별: repository.full_name
        @SuppressWarnings("unchecked")
        Map<String, Object> repo = (Map<String, Object>) payload.get("repository");
        String repoFullName = repo != null ? (String) repo.get("full_name") : null;
        if (repoFullName == null) {
            return ResponseEntity.badRequest().build();
        }

        // 서명 검증 (webhookSecret이 설정된 경우만)
        if (!sig.isBlank() && !webhookService.verifySignature(repoFullName, sig, rawBody)) {
            log.warn("Invalid webhook signature for {}", repoFullName);
            return ResponseEntity.status(401).build();
        }

        switch (event) {
            case "push"         -> webhookService.handlePush(repoFullName, payload);
            case "pull_request" -> webhookService.handlePullRequest(repoFullName, payload);
            default             -> log.debug("Ignored GitHub event: {}", event);
        }

        return ResponseEntity.ok().build();
    }
}
