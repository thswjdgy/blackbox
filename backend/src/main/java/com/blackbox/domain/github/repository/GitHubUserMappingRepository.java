package com.blackbox.domain.github.repository;

import com.blackbox.domain.github.entity.GitHubUserMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GitHubUserMappingRepository extends JpaRepository<GitHubUserMapping, Long> {
    List<GitHubUserMapping> findByProjectId(Long projectId);
    Optional<GitHubUserMapping> findByProjectIdAndGithubLogin(Long projectId, String githubLogin);
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
    void deleteByProjectIdAndId(Long projectId, Long id);
}
