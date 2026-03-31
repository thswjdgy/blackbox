package com.blackbox.domain.vault.service;

import com.blackbox.domain.activity.entity.ActivityLog.EventType;
import com.blackbox.domain.activity.service.ActivityLogService;
import com.blackbox.domain.project.entity.Project;
import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.domain.user.repository.UserRepository;
import com.blackbox.domain.vault.dto.VaultDto;
import com.blackbox.domain.vault.entity.FileVault;
import com.blackbox.domain.vault.entity.TamperDetectionLog;
import com.blackbox.domain.vault.repository.FileVaultRepository;
import com.blackbox.domain.vault.repository.TamperDetectionLogRepository;
import com.blackbox.global.exception.BusinessException;
import com.blackbox.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VaultService {

    private final FileVaultRepository fileVaultRepository;
    private final TamperDetectionLogRepository tamperDetectionLogRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final HashService hashService;
    private final FileStorageService fileStorageService;
    private final ActivityLogService activityLogService;

    @Transactional
    public VaultDto.Response upload(Long projectId, Long userId, MultipartFile file) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        try {
            // 1. SHA-256 해시 계산
            String sha256Hash = hashService.sha256(file.getInputStream());

            // 2. 동일 해시가 이미 있는지 확인 (중복 업로드 감지)
            boolean duplicate = fileVaultRepository.existsByProjectIdAndSha256Hash(projectId, sha256Hash);

            // 3. 같은 파일명의 최신 버전 조회 → 버전 증가
            String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
            int nextVersion = fileVaultRepository
                    .findTopByProjectIdAndFileNameOrderByVersionDesc(projectId, originalFileName)
                    .map(fv -> fv.getVersion() + 1)
                    .orElse(1);

            // 4. 파일 디스크 저장
            String filePath = fileStorageService.store(file, projectId, sha256Hash);

            // 5. file_vault INSERT (DB 트리거로 UPDATE/DELETE 차단됨)
            FileVault vault = FileVault.builder()
                    .project(project)
                    .uploadedBy(user)
                    .fileName(originalFileName)
                    .filePath(filePath)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .sha256Hash(sha256Hash)
                    .version(nextVersion)
                    .build();
            vault = fileVaultRepository.save(vault);

            // 6. activity_log 기록
            activityLogService.logVaultEvent(
                    project, user, EventType.FILE_UPLOADED,
                    Map.of("fileName", originalFileName, "hash", sha256Hash, "version", nextVersion)
            );

            return toResponse(vault, duplicate);

        } catch (IOException e) {
            log.error("File upload failed for project {}: {}", projectId, e.getMessage());
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public List<VaultDto.Response> listFiles(Long projectId) {
        return fileVaultRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(fv -> toResponse(fv, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Resource download(Long vaultId) {
        FileVault vault = fileVaultRepository.findById(vaultId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VAULT_FILE_NOT_FOUND));
        try {
            return fileStorageService.load(vault.getFilePath());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    @Transactional
    public VaultDto.VerifyResponse verify(Long vaultId) {
        FileVault vault = fileVaultRepository.findById(vaultId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VAULT_FILE_NOT_FOUND));

        try {
            String actualHash = hashService.sha256(fileStorageService.openStream(vault.getFilePath()));
            boolean intact = vault.getSha256Hash().equalsIgnoreCase(actualHash);

            if (!intact) {
                // 변조 감지 기록
                TamperDetectionLog tamperLog = TamperDetectionLog.builder()
                        .vault(vault)
                        .expectedHash(vault.getSha256Hash())
                        .actualHash(actualHash)
                        .build();
                tamperDetectionLogRepository.save(tamperLog);

                // activity_log에도 기록
                activityLogService.logVaultEvent(
                        vault.getProject(), vault.getUploadedBy(), EventType.FILE_TAMPERED,
                        Map.of("fileName", vault.getFileName(), "vaultId", vaultId.toString())
                );

                log.warn("TAMPER DETECTED: vaultId={}, file={}", vaultId, vault.getFilePath());
            }

            return VaultDto.VerifyResponse.builder()
                    .vaultId(vault.getId())
                    .fileName(vault.getFileName())
                    .expectedHash(vault.getSha256Hash())
                    .actualHash(actualHash)
                    .intact(intact)
                    .build();

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    private VaultDto.Response toResponse(FileVault vault, boolean duplicate) {
        return VaultDto.Response.builder()
                .id(vault.getId())
                .projectId(vault.getProject().getId())
                .uploadedById(vault.getUploadedBy().getId())
                .fileName(vault.getFileName())
                .fileSize(vault.getFileSize())
                .mimeType(vault.getMimeType())
                .sha256Hash(vault.getSha256Hash())
                .version(vault.getVersion())
                .duplicate(duplicate)
                .createdAt(vault.getCreatedAt())
                .build();
    }
}
