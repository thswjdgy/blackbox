package com.blackbox.domain.notion.repository;

import com.blackbox.domain.notion.entity.NotionUserMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotionUserMappingRepository extends JpaRepository<NotionUserMapping, Long> {
    List<NotionUserMapping>   findByProjectId(Long projectId);
    Optional<NotionUserMapping> findByProjectIdAndNotionUserId(Long projectId, String notionUserId);
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
    void deleteByProjectIdAndId(Long projectId, Long id);
}
