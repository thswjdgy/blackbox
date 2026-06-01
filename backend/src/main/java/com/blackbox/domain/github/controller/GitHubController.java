package com.blackbox.domain.github.controller;

import com.blackbox.domain.github.dto.GitHubDto;
import com.blackbox.domain.github.service.GitHubOAuthService;
import com.blackbox.domain.github.service.GitHubPollService;
import com.blackbox.domain.github.service.GitHubService;
import com.blackbox.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class GitHubController {

    private final GitHubService     githubService;
    private final GitHubPollService pollService;
    private final GitHubOAuthService oauthService;

    @Value("${frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    /** 레포 연동 (PAT + repoFullName) */
    @PostMapping("/projects/{projectId}/github/link")
    public ResponseEntity<GitHubDto.InstallationResponse> link(
            @PathVariable Long projectId,
            @RequestBody GitHubDto.LinkRequest req) {
        return ResponseEntity.ok(githubService.link(projectId, req));
    }

    /** 연동 해제 */
    @DeleteMapping("/projects/{projectId}/github/unlink")
    public ResponseEntity<Void> unlink(@PathVariable Long projectId) {
        githubService.unlink(projectId);
        return ResponseEntity.noContent().build();
    }

    /** 연동 정보 조회 */
    @GetMapping("/projects/{projectId}/github")
    public ResponseEntity<GitHubDto.InstallationResponse> getInstallation(
            @PathVariable Long projectId) {
        GitHubDto.InstallationResponse resp = githubService.getInstallation(projectId);
        return resp != null ? ResponseEntity.ok(resp) : ResponseEntity.notFound().build();
    }

    /** 유저 매핑 목록 */
    @GetMapping("/projects/{projectId}/github/mappings")
    public ResponseEntity<List<GitHubDto.MappingResponse>> getMappings(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(githubService.getMappings(projectId));
    }

    /** 유저 매핑 추가 */
    @PostMapping("/projects/{projectId}/github/mappings")
    public ResponseEntity<GitHubDto.MappingResponse> addMapping(
            @PathVariable Long projectId,
            @RequestBody GitHubDto.MappingRequest req) {
        return ResponseEntity.ok(githubService.addMapping(projectId, req));
    }

    /** 유저 매핑 삭제 */
    @DeleteMapping("/projects/{projectId}/github/mappings/{mappingId}")
    public ResponseEntity<Void> deleteMapping(
            @PathVariable Long projectId,
            @PathVariable Long mappingId) {
        githubService.deleteMapping(projectId, mappingId);
        return ResponseEntity.noContent().build();
    }

    /** 수동 즉시 폴링 */
    @PostMapping("/projects/{projectId}/github/poll")
    public ResponseEntity<GitHubDto.PollResult> poll(@PathVariable Long projectId) {
        return ResponseEntity.ok(pollService.pollNow(projectId));
    }

    /* ── GitHub OAuth ── */

    /** GitHub OAuth 인증 URL 반환 (state = projectId:userId) */
    @GetMapping("/projects/{projectId}/github/oauth/auth")
    public ResponseEntity<Map<String, String>> getOAuthUrl(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("url", oauthService.buildAuthUrl(projectId, user.getId())));
    }

    /** GitHub OAuth 콜백 — GitHub이 직접 호출 (인증 불필요) */
    @GetMapping("/github/oauth/callback")
    public ResponseEntity<Void> oauthCallback(
            @RequestParam String code,
            @RequestParam String state) {
        String[] parts = state.split(":");
        Long projectId = Long.parseLong(parts[0]);
        oauthService.handleCallback(code, state);
        return ResponseEntity.status(302)
                .location(URI.create(frontendBaseUrl + "/oauth/github/success?projectId=" + projectId))
                .build();
    }
}
