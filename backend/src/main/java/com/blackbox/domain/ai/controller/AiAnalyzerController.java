package com.blackbox.domain.ai.controller;

import com.blackbox.domain.ai.service.AiAnalyzerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects/{projectId}/ai-analysis")
@RequiredArgsConstructor
public class AiAnalyzerController {

    private final AiAnalyzerService aiAnalyzerService;

    /** POST — 팀 활동 AI 분석 (새로 실행) */
    @PostMapping
    public ResponseEntity<Map<String, Object>> analyze(@PathVariable Long projectId) {
        String result = aiAnalyzerService.analyze(projectId);
        return ResponseEntity.ok(Map.of("analysis", result, "createdAt", Instant.now().toString()));
    }

    /** GET — 마지막 저장된 팀 분석 결과 */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getLatestAnalysis(@PathVariable Long projectId) {
        return aiAnalyzerService.getLatestTeamAnalysis(projectId)
                .map(r -> ResponseEntity.ok(Map.<String, Object>of(
                        "analysis", r.getResult(),
                        "createdAt", r.getCreatedAt().toString())))
                .orElse(ResponseEntity.noContent().build());
    }

    /** POST — 커밋 품질 분석 (새로 실행) */
    @PostMapping("/commits")
    public ResponseEntity<Map<String, Object>> analyzeCommits(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> commits = aiAnalyzerService.analyzeCommitQuality(projectId, limit);
        return ResponseEntity.ok(Map.of("commits", commits, "createdAt", Instant.now().toString()));
    }

    /** GET — 마지막 저장된 커밋 분석 결과 */
    @GetMapping("/commits")
    public ResponseEntity<Map<String, Object>> getLatestCommitAnalysis(@PathVariable Long projectId) {
        return aiAnalyzerService.getLatestCommitResult(projectId)
                .map(r -> ResponseEntity.ok(Map.<String, Object>of(
                        "commits",   aiAnalyzerService.parseCommitResult(r.getResult()),
                        "createdAt", r.getCreatedAt().toString())))
                .orElse(ResponseEntity.noContent().build());
    }
}
