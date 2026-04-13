package com.blackbox.domain.google.repository;

import com.blackbox.domain.google.entity.GoogleUserMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoogleUserMappingRepository extends JpaRepository<GoogleUserMapping, Long> {
    List<GoogleUserMapping> findByProjectId(Long projectId);
    Optional<GoogleUserMapping> findByProjectIdAndGoogleEmail(Long projectId, String googleEmail);
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
}
