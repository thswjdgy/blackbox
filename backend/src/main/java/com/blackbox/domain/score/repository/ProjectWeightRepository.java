package com.blackbox.domain.score.repository;

import com.blackbox.domain.score.entity.ProjectWeight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectWeightRepository extends JpaRepository<ProjectWeight, Long> {
    Optional<ProjectWeight> findByProjectId(Long projectId);
}
