package com.blackbox.domain.manual.controller;

import com.blackbox.domain.manual.dto.ManualLogDto;
import com.blackbox.domain.manual.service.ManualLogService;
import com.blackbox.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/manual-logs")
@RequiredArgsConstructor
public class ManualLogController {

    private final ManualLogService manualLogService;

    /** POST /api/projects/{projectId}/manual-logs — 수동 작업 신고 */
    @PostMapping
    public ResponseEntity<ManualLogDto.Response> submit(
            @PathVariable Long projectId,
            @Valid @RequestBody ManualLogDto.CreateRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manualLogService.submit(projectId, user, req));
    }

    /** GET /api/projects/{projectId}/manual-logs — 전체 목록 (팀장) */
    @GetMapping
    public ResponseEntity<List<ManualLogDto.Response>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(manualLogService.list(projectId));
    }

    /** GET /api/projects/{projectId}/manual-logs/me — 내 신고 목록 */
    @GetMapping("/me")
    public ResponseEntity<List<ManualLogDto.Response>> myList(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(manualLogService.myList(projectId, user.getId()));
    }

    /** PATCH /api/projects/{projectId}/manual-logs/{logId}/review — 승인/거절 (팀장) */
    @PatchMapping("/{logId}/review")
    public ResponseEntity<ManualLogDto.Response> review(
            @PathVariable Long projectId,
            @PathVariable Long logId,
            @RequestBody ManualLogDto.ReviewRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(manualLogService.review(projectId, logId, user, req));
    }
}
