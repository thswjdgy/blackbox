package com.blackbox.domain.score.repository;

import com.blackbox.domain.score.entity.WeightHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeightHistoryRepository extends JpaRepository<WeightHistory, Long> {
    List<WeightHistory> findByProjectIdOrderByChangedAtDesc(Long projectId);
}
