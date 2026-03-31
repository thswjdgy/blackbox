package com.blackbox.domain.vault.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

public class VaultDto {

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long projectId;
        private Long uploadedById;
        private String fileName;
        private Long fileSize;
        private String mimeType;
        private String sha256Hash;
        private int version;
        private boolean duplicate;   // 동일 해시가 이미 프로젝트에 존재했을 경우 true
        private Instant createdAt;
    }

    @Getter
    @Builder
    public static class VerifyResponse {
        private Long vaultId;
        private String fileName;
        private String expectedHash;
        private String actualHash;
        private boolean intact;       // true: 파일 정상, false: 변조 감지
    }
}
