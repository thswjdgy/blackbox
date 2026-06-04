package com.blackbox.domain.score.service;

import com.blackbox.domain.score.entity.Alert;
import com.blackbox.domain.score.entity.ProjectAlertConfig;
import com.blackbox.domain.score.repository.ProjectAlertConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordNotificationService {

    private final RestTemplate restTemplate;
    private final ProjectAlertConfigRepository alertConfigRepository;

    private static final Map<String, Integer> SEVERITY_COLOR = Map.of(
            "CRITICAL", 0xED4245,
            "WARNING",  0xFEE75C,
            "INFO",     0x5865F2
    );
    private static final Map<Alert.AlertType, String> TYPE_LABEL = Map.of(
            Alert.AlertType.IMBALANCE,  "기여 불균형",
            Alert.AlertType.INACTIVITY, "무활동 이탈",
            Alert.AlertType.OVERLOAD,   "과부하",
            Alert.AlertType.CRAMMING,   "벼락치기",
            Alert.AlertType.DEADLINE,   "마감 임박"
    );
    private static final Map<String, String> SEVERITY_ICON = Map.of(
            "CRITICAL", "🚨",
            "WARNING",  "⚠️",
            "INFO",     "ℹ️"
    );

    /* ── 경보 알림 ── */
    public void sendAlert(String webhookUrl, String projectName,
                          Alert.AlertType alertType, String severity, String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        String icon  = SEVERITY_ICON.getOrDefault(severity, "⚠️");
        String label = TYPE_LABEL.getOrDefault(alertType, alertType.name());
        int color    = SEVERITY_COLOR.getOrDefault(severity, 0xFEE75C);
        sendEmbed(webhookUrl, icon + " " + label + " 경보", message, color,
                "📦 Blackbox · " + projectName);
    }

    /* ── 타임라인 업데이트 알림 (projectId로 URL 자동 조회) ── */
    public void sendUpdate(Long projectId, String projectName,
                           String title, String description, int color) {
        String url = getWebhookUrl(projectId);
        if (url == null) return;
        sendEmbed(url, title, description, color, "📦 Blackbox · " + projectName);
    }

    /* ── 테스트 메시지 ── */
    public void sendTest(String webhookUrl, String projectName) {
        sendEmbed(webhookUrl,
                "✅ Blackbox 연동 완료",
                "Discord 알림이 정상적으로 설정됐습니다.",
                0x57F287,
                "📦 Blackbox · " + projectName);
    }

    /* ── 내부 헬퍼 ── */
    private String getWebhookUrl(Long projectId) {
        return alertConfigRepository.findByProjectId(projectId)
                .map(ProjectAlertConfig::getDiscordWebhookUrl)
                .filter(u -> u != null && !u.isBlank())
                .orElse(null);
    }

    private void sendEmbed(String webhookUrl, String title, String description,
                           int color, String footerText) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        try {
            Map<String, Object> embed = Map.of(
                    "title",       title,
                    "description", description,
                    "color",       color,
                    "footer",      Map.of("text", footerText),
                    "timestamp",   java.time.Instant.now().toString()
            );
            Map<String, Object> body = Map.of(
                    "username", "Blackbox",
                    "embeds",   List.of(embed)
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(webhookUrl, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            log.warn("Discord 전송 실패: {}", e.getMessage());
        }
    }
}
