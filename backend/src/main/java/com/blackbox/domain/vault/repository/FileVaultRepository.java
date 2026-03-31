package com.blackbox.domain.vault.repository;

import com.blackbox.domain.vault.entity.FileVault;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileVaultRepository extends JpaRepository<FileVault, Long> {
    List<FileVault> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    Optional<FileVault> findTopByProjectIdAndFileNameOrderByVersionDesc(Long projectId, String fileName);
    boolean existsByProjectIdAndSha256Hash(Long projectId, String sha256Hash);
}
