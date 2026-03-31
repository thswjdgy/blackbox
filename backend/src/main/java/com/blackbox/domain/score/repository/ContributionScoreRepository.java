package com.blackbox.domain.score.repository;

import com.blackbox.domain.score.entity.ContributionScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContributionScoreRepository extends JpaRepository<ContributionScore, Long> {
    List<ContributionScore> findByProjectIdOrderByNormalizedScoreDesc(Long projectId);
    Optional<ContributionScore> findByProjectIdAndUserId(Long projectId, Long userId);
}
