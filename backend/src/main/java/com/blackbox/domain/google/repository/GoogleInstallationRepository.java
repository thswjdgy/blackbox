package com.blackbox.domain.google.repository;

import com.blackbox.domain.google.entity.GoogleInstallation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoogleInstallationRepository extends JpaRepository<GoogleInstallation, Long> {
    Optional<GoogleInstallation> findByProjectId(Long projectId);
    List<GoogleInstallation> findAllByRefreshTokenIsNotNull();
}
