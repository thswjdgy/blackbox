package com.blackbox.domain.manual.repository;

import com.blackbox.domain.manual.entity.ManualLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManualLogRepository extends JpaRepository<ManualLog, Long> {
    List<ManualLog> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<ManualLog> findByProjectIdAndUserIdOrderByCreatedAtDesc(Long projectId, Long userId);
    List<ManualLog> findByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, String status);
}
