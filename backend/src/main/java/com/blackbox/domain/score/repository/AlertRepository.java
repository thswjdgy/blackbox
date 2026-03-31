package com.blackbox.domain.score.repository;

import com.blackbox.domain.score.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByProjectIdAndIsResolvedFalseOrderByCreatedAtDesc(Long projectId);
    List<Alert> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    boolean existsByProjectIdAndAlertTypeAndIsResolvedFalse(Long projectId, Alert.AlertType alertType);
}
