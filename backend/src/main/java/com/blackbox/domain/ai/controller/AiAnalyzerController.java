package com.blackbox.domain.ai.controller;

import com.blackbox.domain.ai.service.AiAnalyzerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/projects/{projectId}/ai-analysis")
@RequiredArgsConstructor
public class AiAnalyzerController {

    private final AiAnalyzerService aiAnalyzerService;

    /** POST /api/projects/{projectId}/ai-analysis — 팀 활동 AI 분석 */
    @PostMapping
    public ResponseEntity<Map<String, String>> analyze(@PathVariable Long projectId) {
        String result = aiAnalyzerService.analyze(projectId);
        return ResponseEntity.ok(Map.of("analysis", result));
    }
}
