package com.blackbox.domain.score.controller;

import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.score.entity.ProjectAlertConfig;
import com.blackbox.domain.score.repository.ProjectAlertConfigRepository;
import com.blackbox.domain.score.service.DiscordNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/projects/{projectId}/discord")
@RequiredArgsConstructor
public class DiscordController {

    private final ProjectAlertConfigRepository alertConfigRepository;
    private final ProjectRepository            projectRepository;
    private final DiscordNotificationService   discordService;

    /** Discord webhook URL 조회 */
    @GetMapping
    public ResponseEntity<Map<String, String>> getWebhook(@PathVariable Long projectId) {
        String url = alertConfigRepository.findByProjectId(projectId)
                .map(ProjectAlertConfig::getDiscordWebhookUrl)
                .orElse("");
        return ResponseEntity.ok(Map.of("webhookUrl", url != null ? url : ""));
    }

    /** Discord webhook URL 저장 */
    @PutMapping
    public ResponseEntity<Void> saveWebhook(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> body) {
        var project = projectRepository.findById(projectId).orElseThrow();
        ProjectAlertConfig cfg = alertConfigRepository.findByProjectId(projectId)
                .orElseGet(() -> ProjectAlertConfig.builder().project(project).build());
        cfg.setDiscordWebhookUrl(body.getOrDefault("webhookUrl", ""));
        alertConfigRepository.save(cfg);
        return ResponseEntity.noContent().build();
    }

    /** 테스트 메시지 전송 */
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testWebhook(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> body) {
        String url  = body.getOrDefault("webhookUrl", "");
        String name = projectRepository.findById(projectId)
                .map(p -> p.getName()).orElse("프로젝트");
        if (url.isBlank()) return ResponseEntity.badRequest()
                .body(Map.of("error", "URL을 입력하세요."));
        try {
            discordService.sendTest(url, name);
            return ResponseEntity.ok(Map.of("result", "ok"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
