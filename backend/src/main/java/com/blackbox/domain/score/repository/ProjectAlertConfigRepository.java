package com.blackbox.domain.score.repository;

import com.blackbox.domain.score.entity.ProjectAlertConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectAlertConfigRepository extends JpaRepository<ProjectAlertConfig, Long> {
    Optional<ProjectAlertConfig> findByProjectId(Long projectId);
}
