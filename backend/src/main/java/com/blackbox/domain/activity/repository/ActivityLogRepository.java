package com.blackbox.domain.activity.repository;

import com.blackbox.domain.activity.entity.ActivityLog;
import com.blackbox.domain.activity.entity.ActivityLog.EventType;
import org.springframework.data.domain.Pageable;
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

    /** 타임라인: 프로젝트 전체 (페이지네이션) */
    List<ActivityLog> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);

    /** 타임라인: 소스 필터 */
    List<ActivityLog> findByProjectIdAndSourceOrderByCreatedAtDesc(Long projectId, String source, Pageable pageable);

    /** 타임라인: 유저 필터 */
    List<ActivityLog> findByProjectIdAndUserIdOrderByCreatedAtDesc(Long projectId, Long userId, Pageable pageable);

    /** 타임라인: 소스 + 유저 필터 */
    List<ActivityLog> findByProjectIdAndSourceAndUserIdOrderByCreatedAtDesc(Long projectId, String source, Long userId, Pageable pageable);

    /**
     * 프로젝트 내 유저별 이벤트 타입 가중 합산
     * GitHub 이벤트는 payload->>'scoreWeight' (파일 수 기반)을 추가 가중치로 사용.
     * scoreWeight 없는 이벤트는 1.0으로 fallback.
     */
    @Query(value = """
        SELECT a.user_id,
               a.event_type,
               SUM(COALESCE(CAST(a.payload->>'scoreWeight' AS FLOAT), 1.0) * a.trust_level)
        FROM activity_logs a
        WHERE a.project_id = :projectId
        GROUP BY a.user_id, a.event_type
        """, nativeQuery = true)
    List<Object[]> countByProjectGroupByUserAndType(@Param("projectId") Long projectId);

    /** 특정 이벤트 타입만 조회 (커밋 품질 분석용) */
    List<ActivityLog> findByProjectIdAndEventTypeOrderByCreatedAtDesc(
            Long projectId, ActivityLog.EventType eventType, Pageable pageable);

    /** 벼락치기 감지: 특정 유저의 전체 이벤트 수 */
    long countByProjectIdAndUserId(Long projectId, Long userId);

    /** 벼락치기 감지: 특정 유저의 최근 N일 이벤트 수 */
    long countByProjectIdAndUserIdAndCreatedAtAfter(Long projectId, Long userId, java.time.Instant since);

    /** 특정 유저의 최근 활동 여부 (N일 이내) */
    boolean existsByProjectIdAndUserIdAndCreatedAtAfter(
            Long projectId, Long userId, Instant since);

    /** GitHub 이벤트 중복 방지: payload->>'sha' 기반 체크 */
    @Query(value = """
            SELECT COUNT(*) > 0
            FROM activity_logs
            WHERE project_id = :projectId
              AND source = :source
              AND payload->>'sha' = :sha
            """, nativeQuery = true)
    boolean existsByProjectIdAndSourceAndPayloadSha(
            @Param("projectId") Long projectId,
            @Param("source") String source,
            @Param("sha") String sha);
}
