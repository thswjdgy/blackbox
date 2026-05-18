package com.blackbox.domain.ai.repository;

import com.blackbox.domain.ai.entity.AiAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiAnalysisResultRepository extends JpaRepository<AiAnalysisResult, Long> {
    Optional<AiAnalysisResult> findTopByProjectIdAndAnalysisTypeOrderByCreatedAtDesc(
            Long projectId, AiAnalysisResult.AnalysisType analysisType);

    List<AiAnalysisResult> findByProjectIdAndAnalysisTypeOrderByCreatedAtDesc(
            Long projectId, AiAnalysisResult.AnalysisType analysisType);
}
