package com.blackbox.domain.vault.repository;

import com.blackbox.domain.vault.entity.TamperDetectionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TamperDetectionLogRepository extends JpaRepository<TamperDetectionLog, Long> {
    List<TamperDetectionLog> findByVaultIdOrderByDetectedAtDesc(Long vaultId);
}
