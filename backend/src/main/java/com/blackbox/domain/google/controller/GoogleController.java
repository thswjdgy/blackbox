package com.blackbox.domain.google.controller;

import com.blackbox.domain.google.dto.GoogleDto;
import com.blackbox.domain.google.service.GoogleOAuthService;
import com.blackbox.domain.google.service.GooglePollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class GoogleController {

    private final GoogleOAuthService oauthService;
    private final GooglePollService  pollService;

    @Value("${frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    /* ── OAuth ── */

    /** Google OAuth 인증 URL 반환 (state = projectId) */
    @GetMapping("/projects/{projectId}/google/auth")
    public ResponseEntity<Map<String, String>> getAuthUrl(@PathVariable Long projectId) {
        return ResponseEntity.ok(Map.of("url", oauthService.buildAuthUrl(projectId)));
    }

    /** Google OAuth 콜백 — 인증 불필요, Google이 직접 호출 */
    @GetMapping("/google/oauth/callback")
    public ResponseEntity<Void> oauthCallback(
            @RequestParam String code,
            @RequestParam String state) {
        Long projectId = Long.parseLong(state);
        oauthService.handleCallback(code, projectId);
        // 프론트엔드 설정 페이지로 리다이렉트
        return ResponseEntity.status(302)
                .location(URI.create(frontendBaseUrl + "/projects/" + projectId + "/settings?tab=google"))
                .build();
    }

    /* ── 연동 정보 ── */

    @GetMapping("/projects/{projectId}/google")
    public ResponseEntity<GoogleDto.InstallationResponse> getInstallation(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(oauthService.getInstallation(projectId));
    }

    @DeleteMapping("/projects/{projectId}/google/unlink")
    public ResponseEntity<Void> unlink(@PathVariable Long projectId) {
        oauthService.unlink(projectId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/projects/{projectId}/google/resources")
    public ResponseEntity<GoogleDto.InstallationResponse> updateResources(
            @PathVariable Long projectId,
            @RequestBody GoogleDto.ResourceRequest req) {
        return ResponseEntity.ok(oauthService.updateResources(projectId, req));
    }

    /* ── 유저 매핑 ── */

    @GetMapping("/projects/{projectId}/google/mappings")
    public ResponseEntity<List<GoogleDto.MappingResponse>> getMappings(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(oauthService.getMappings(projectId));
    }

    @PostMapping("/projects/{projectId}/google/mappings")
    public ResponseEntity<GoogleDto.MappingResponse> addMapping(
            @PathVariable Long projectId,
            @RequestBody GoogleDto.MappingRequest req) {
        return ResponseEntity.ok(oauthService.addMapping(projectId, req));
    }

    @DeleteMapping("/projects/{projectId}/google/mappings/{mappingId}")
    public ResponseEntity<Void> deleteMapping(
            @PathVariable Long projectId,
            @PathVariable Long mappingId) {
        oauthService.deleteMapping(projectId, mappingId);
        return ResponseEntity.noContent().build();
    }

    /* ── 폴링 ── */

    @PostMapping("/projects/{projectId}/google/poll")
    public ResponseEntity<GoogleDto.PollResult> poll(@PathVariable Long projectId) {
        return ResponseEntity.ok(pollService.pollNow(projectId));
    }
}
