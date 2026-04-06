package com.blackbox.domain.github.controller;

import com.blackbox.domain.github.dto.GitHubDto;
import com.blackbox.domain.github.service.GitHubPollService;
import com.blackbox.domain.github.service.GitHubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/github")
@RequiredArgsConstructor
public class GitHubController {

    private final GitHubService     githubService;
    private final GitHubPollService pollService;

    /** 레포 연동 (PAT + repoFullName) */
    @PostMapping("/link")
    public ResponseEntity<GitHubDto.InstallationResponse> link(
            @PathVariable Long projectId,
            @RequestBody GitHubDto.LinkRequest req) {
        return ResponseEntity.ok(githubService.link(projectId, req));
    }

    /** 연동 해제 */
    @DeleteMapping("/unlink")
    public ResponseEntity<Void> unlink(@PathVariable Long projectId) {
        githubService.unlink(projectId);
        return ResponseEntity.noContent().build();
    }

    /** 연동 정보 조회 */
    @GetMapping
    public ResponseEntity<GitHubDto.InstallationResponse> getInstallation(
            @PathVariable Long projectId) {
        GitHubDto.InstallationResponse resp = githubService.getInstallation(projectId);
        return resp != null ? ResponseEntity.ok(resp) : ResponseEntity.notFound().build();
    }

    /** 유저 매핑 목록 */
    @GetMapping("/mappings")
    public ResponseEntity<List<GitHubDto.MappingResponse>> getMappings(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(githubService.getMappings(projectId));
    }

    /** 유저 매핑 추가 */
    @PostMapping("/mappings")
    public ResponseEntity<GitHubDto.MappingResponse> addMapping(
            @PathVariable Long projectId,
            @RequestBody GitHubDto.MappingRequest req) {
        return ResponseEntity.ok(githubService.addMapping(projectId, req));
    }

    /** 유저 매핑 삭제 */
    @DeleteMapping("/mappings/{mappingId}")
    public ResponseEntity<Void> deleteMapping(
            @PathVariable Long projectId,
            @PathVariable Long mappingId) {
        githubService.deleteMapping(projectId, mappingId);
        return ResponseEntity.noContent().build();
    }

    /** 수동 즉시 폴링 */
    @PostMapping("/poll")
    public ResponseEntity<GitHubDto.PollResult> poll(@PathVariable Long projectId) {
        return ResponseEntity.ok(pollService.pollNow(projectId));
    }
}
