package com.blackbox.domain.github.repository;

import com.blackbox.domain.github.entity.GitHubInstallation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GitHubInstallationRepository extends JpaRepository<GitHubInstallation, Long> {
    Optional<GitHubInstallation> findByProjectId(Long projectId);
    Optional<GitHubInstallation> findByRepoFullName(String repoFullName);
    List<GitHubInstallation> findAllByGithubTokenIsNotNull();
}
