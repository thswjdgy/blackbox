package com.blackbox.domain.score.controller;

import com.blackbox.domain.score.dto.ScoreDto;
import com.blackbox.domain.score.entity.Alert;
import com.blackbox.domain.score.repository.AlertRepository;
import com.blackbox.domain.score.service.ScoreEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/projects/{projectId}")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreEngine scoreEngine;
    private final AlertRepository alertRepository;

    /** 기여도 리포트 조회 */
    @GetMapping("/scores")
    public ResponseEntity<ScoreDto.ProjectScoreReport> getScores(@PathVariable Long projectId) {
        return ResponseEntity.ok(scoreEngine.getReport(projectId));
    }

    /** 수동 점수 재계산 */
    @PostMapping("/scores/calculate")
    public ResponseEntity<ScoreDto.ProjectScoreReport> calculate(@PathVariable Long projectId) {
        return ResponseEntity.ok(scoreEngine.calculate(projectId));
    }

    /** 미해결 경보 목록 */
    @GetMapping("/alerts")
    public ResponseEntity<List<ScoreDto.AlertResponse>> getAlerts(@PathVariable Long projectId) {
        List<ScoreDto.AlertResponse> alerts = alertRepository
                .findByProjectIdAndIsResolvedFalseOrderByCreatedAtDesc(projectId)
                .stream().map(this::toAlertResponse).collect(Collectors.toList());
        return ResponseEntity.ok(alerts);
    }

    /** 경보 해결 처리 */
    @PatchMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<Void> resolveAlert(@PathVariable Long projectId, @PathVariable Long alertId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.setResolved(true);
            alert.setResolvedAt(Instant.now());
            alertRepository.save(alert);
        });
        return ResponseEntity.noContent().build();
    }

    private ScoreDto.AlertResponse toAlertResponse(Alert a) {
        return ScoreDto.AlertResponse.builder()
                .id(a.getId())
                .projectId(a.getProject().getId())
                .alertType(a.getAlertType())
                .severity(a.getSeverity())
                .message(a.getMessage())
                .resolved(a.isResolved())
                .createdAt(a.getCreatedAt())
                .resolvedAt(a.getResolvedAt())
                .build();
    }
}
