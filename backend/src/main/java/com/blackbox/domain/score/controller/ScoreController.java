package com.blackbox.domain.score.controller;

import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.score.dto.ScoreDto;
import com.blackbox.domain.score.dto.WeightDto;
import com.blackbox.domain.score.entity.Alert;
import com.blackbox.domain.score.entity.ProjectWeight;
import com.blackbox.domain.score.entity.WeightHistory;
import com.blackbox.domain.score.repository.AlertRepository;
import com.blackbox.domain.score.repository.ProjectWeightRepository;
import com.blackbox.domain.score.repository.WeightHistoryRepository;
import com.blackbox.domain.score.service.PdfReportService;
import com.blackbox.domain.score.service.ScoreEngine;
import com.blackbox.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/projects/{projectId}")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreEngine scoreEngine;
    private final PdfReportService pdfReportService;
    private final AlertRepository alertRepository;
    private final ProjectWeightRepository weightRepository;
    private final WeightHistoryRepository weightHistoryRepository;
    private final ProjectRepository projectRepository;

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

    /** 기여도 리포트 PDF 다운로드 */
    @GetMapping("/report/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long projectId) {
        ScoreDto.ProjectScoreReport report = scoreEngine.getReport(projectId);
        byte[] pdf = pdfReportService.generate(projectId, report);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"contribution-report-" + projectId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** 미해결 경보 목록 */
    @GetMapping("/alerts")
    public ResponseEntity<List<ScoreDto.AlertResponse>> getAlerts(@PathVariable Long projectId) {
        List<ScoreDto.AlertResponse> alerts = alertRepository
                .findByProjectIdAndIsResolvedFalseOrderByCreatedAtDesc(projectId)
                .stream().map(this::toAlertResponse).collect(Collectors.toList());
        return ResponseEntity.ok(alerts);
    }

    /** 가중치 조회 (없으면 기본값 반환) */
    @GetMapping("/weights")
    public ResponseEntity<WeightDto.Response> getWeights(@PathVariable Long projectId) {
        ProjectWeight w = weightRepository.findByProjectId(projectId).orElseGet(() -> {
            var project = projectRepository.findById(projectId).orElseThrow();
            return ProjectWeight.builder().project(project).build();
        });
        return ResponseEntity.ok(toWeightResponse(w, projectId));
    }

    /** 가중치 저장/수정 */
    @PutMapping("/weights")
    public ResponseEntity<WeightDto.Response> updateWeights(
            @PathVariable Long projectId,
            @RequestBody java.util.Map<String, Double> body,
            @AuthenticationPrincipal User user) {
        double wTask    = body.getOrDefault("wTask",    0.35);
        double wMeeting = body.getOrDefault("wMeeting", 0.30);
        double wFile    = body.getOrDefault("wFile",    0.20);
        double wExtra   = body.getOrDefault("wExtra",   0.15);

        var project = projectRepository.findById(projectId).orElseThrow();
        ProjectWeight w = weightRepository.findByProjectId(projectId).orElseGet(() ->
                ProjectWeight.builder().project(project).build());
        w.setWTask(wTask);
        w.setWMeeting(wMeeting);
        w.setWFile(wFile);
        w.setWExtra(wExtra);
        w.setUpdatedAt(Instant.now());
        weightRepository.save(w);

        // 변경 이력 저장
        weightHistoryRepository.save(WeightHistory.builder()
                .project(project).changedBy(user)
                .wTask(wTask).wMeeting(wMeeting)
                .wFile(wFile).wExtra(wExtra)
                .build());

        return ResponseEntity.ok(toWeightResponse(w, projectId));
    }

    /** 가중치 변경 이력 조회 */
    @GetMapping("/weights/history")
    public ResponseEntity<List<WeightDto.HistoryResponse>> getWeightHistory(@PathVariable Long projectId) {
        List<WeightDto.HistoryResponse> history = weightHistoryRepository
                .findByProjectIdOrderByChangedAtDesc(projectId)
                .stream()
                .map(h -> WeightDto.HistoryResponse.builder()
                        .id(h.getId())
                        .projectId(projectId)
                        .changedById(h.getChangedBy().getId())
                        .changedByName(h.getChangedBy().getName())
                        .wTask(h.getWTask()).wMeeting(h.getWMeeting())
                        .wFile(h.getWFile()).wExtra(h.getWExtra())
                        .changedAt(h.getChangedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    private WeightDto.Response toWeightResponse(ProjectWeight w, Long projectId) {
        return WeightDto.Response.builder()
                .projectId(projectId)
                .wTask(w.getWTask())
                .wMeeting(w.getWMeeting())
                .wFile(w.getWFile())
                .wExtra(w.getWExtra())
                .updatedAt(w.getUpdatedAt())
                .build();
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
