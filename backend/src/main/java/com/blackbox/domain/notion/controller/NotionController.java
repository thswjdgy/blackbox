package com.blackbox.domain.notion.controller;

import com.blackbox.domain.notion.dto.NotionDto;
import com.blackbox.domain.notion.service.NotionPollService;
import com.blackbox.domain.notion.service.NotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/notion")
@RequiredArgsConstructor
public class NotionController {

    private final NotionService     notionService;
    private final NotionPollService pollService;

    /** 연동 (Integration Token 저장) */
    @PostMapping("/link")
    public ResponseEntity<NotionDto.InstallationResponse> link(
            @PathVariable Long projectId,
            @RequestBody NotionDto.LinkRequest req) {
        return ResponseEntity.ok(notionService.link(projectId, req));
    }

    /** 연동 해제 */
    @DeleteMapping("/unlink")
    public ResponseEntity<Void> unlink(@PathVariable Long projectId) {
        notionService.unlink(projectId);
        return ResponseEntity.noContent().build();
    }

    /** 연동 정보 조회 */
    @GetMapping
    public ResponseEntity<NotionDto.InstallationResponse> getInstallation(
            @PathVariable Long projectId) {
        NotionDto.InstallationResponse resp = notionService.getInstallation(projectId);
        return resp != null ? ResponseEntity.ok(resp) : ResponseEntity.notFound().build();
    }

    /** 유저 매핑 목록 */
    @GetMapping("/mappings")
    public ResponseEntity<List<NotionDto.MappingResponse>> getMappings(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(notionService.getMappings(projectId));
    }

    /** 유저 매핑 추가 */
    @PostMapping("/mappings")
    public ResponseEntity<NotionDto.MappingResponse> addMapping(
            @PathVariable Long projectId,
            @RequestBody NotionDto.MappingRequest req) {
        return ResponseEntity.ok(notionService.addMapping(projectId, req));
    }

    /** 유저 매핑 삭제 */
    @DeleteMapping("/mappings/{mappingId}")
    public ResponseEntity<Void> deleteMapping(
            @PathVariable Long projectId,
            @PathVariable Long mappingId) {
        notionService.deleteMapping(projectId, mappingId);
        return ResponseEntity.noContent().build();
    }

    /** 수동 즉시 폴링 */
    @PostMapping("/poll")
    public ResponseEntity<NotionDto.PollResult> poll(@PathVariable Long projectId) {
        return ResponseEntity.ok(pollService.pollNow(projectId));
    }
}
