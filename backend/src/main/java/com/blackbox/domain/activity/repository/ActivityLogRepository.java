package com.blackbox.domain.activity.repository;

import com.blackbox.domain.activity.entity.ActivityLog;
import com.blackbox.domain.activity.entity.ActivityLog.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 프로젝트 내 유저별 이벤트 타입 건수 집계 */
    @Query("""
        SELECT a.user.id, a.eventType, COUNT(a)
        FROM ActivityLog a
        WHERE a.project.id = :projectId
        GROUP BY a.user.id, a.eventType
        """)
    List<Object[]> countByProjectGroupByUserAndType(@Param("projectId") Long projectId);

    /** 특정 유저의 최근 활동 여부 (N일 이내) */
    boolean existsByProjectIdAndUserIdAndCreatedAtAfter(
            Long projectId, Long userId, Instant since);
}
