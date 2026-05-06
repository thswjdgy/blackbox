package com.blackbox.domain.review.repository;

import com.blackbox.domain.review.entity.PeerReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeerReviewRepository extends JpaRepository<PeerReview, Long> {

    List<PeerReview> findByProjectIdAndReviewerId(Long projectId, Long reviewerId);

    boolean existsByProjectIdAndReviewerIdAndRevieweeId(Long projectId, Long reviewerId, Long revieweeId);

    /** 피평가자별 평균 점수 집계 */
    @Query("""
        SELECT r.reviewee.id, AVG(r.score), COUNT(r)
        FROM PeerReview r
        WHERE r.project.id = :projectId
        GROUP BY r.reviewee.id
        """)
    List<Object[]> avgScoreByReviewee(@Param("projectId") Long projectId);

    List<PeerReview> findByProjectIdAndRevieweeId(Long projectId, Long revieweeId);
}
