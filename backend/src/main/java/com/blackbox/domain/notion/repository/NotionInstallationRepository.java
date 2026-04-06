package com.blackbox.domain.notion.repository;

import com.blackbox.domain.notion.entity.NotionInstallation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotionInstallationRepository extends JpaRepository<NotionInstallation, Long> {
    Optional<NotionInstallation> findByProjectId(Long projectId);
    List<NotionInstallation> findAllByIntegrationTokenIsNotNull();
}
