package com.blackbox.domain.vault.controller;

import com.blackbox.domain.user.entity.User;
import com.blackbox.domain.vault.dto.VaultDto;
import com.blackbox.domain.vault.service.VaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    /** 파일 업로드 */
    @PostMapping("/projects/{projectId}/files")
    public ResponseEntity<VaultDto.Response> upload(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vaultService.upload(projectId, user.getId(), file));
    }

    /** 프로젝트 파일 목록 */
    @GetMapping("/projects/{projectId}/files")
    public ResponseEntity<List<VaultDto.Response>> listFiles(@PathVariable Long projectId) {
        return ResponseEntity.ok(vaultService.listFiles(projectId));
    }

    /** 파일 다운로드 */
    @GetMapping("/files/{vaultId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long vaultId) {
        Resource resource = vaultService.download(vaultId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /** 해시 무결성 검증 */
    @GetMapping("/files/{vaultId}/verify")
    public ResponseEntity<VaultDto.VerifyResponse> verify(@PathVariable Long vaultId) {
        return ResponseEntity.ok(vaultService.verify(vaultId));
    }
}
