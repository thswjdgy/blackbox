package com.blackbox.domain.review.service;

import com.blackbox.domain.project.repository.ProjectMemberRepository;
import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.review.dto.PeerReviewDto;
import com.blackbox.domain.review.entity.PeerReview;
import com.blackbox.domain.review.repository.PeerReviewRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.domain.user.repository.UserRepository;
import com.blackbox.global.exception.BusinessException;
import com.blackbox.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PeerReviewService {

    private final PeerReviewRepository reviewRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    /** 피어리뷰 제출 (본인 제외, 1회만) */
    @Transactional
    public PeerReviewDto.Response submit(Long projectId, User reviewer, PeerReviewDto.CreateRequest req) {
        if (reviewer.getId().equals(req.getRevieweeId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "자기 자신은 평가할 수 없습니다.");
        }
        if (reviewRepository.existsByProjectIdAndReviewerIdAndRevieweeId(
                projectId, reviewer.getId(), req.getRevieweeId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 해당 멤버를 평가했습니다.");
        }

        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        var reviewee = userRepository.findById(req.getRevieweeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        PeerReview review = PeerReview.builder()
                .project(project)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .score(req.getScore())
                .comment(req.getComment())
                .isAnonymous(req.isAnonymous())
                .build();
        reviewRepository.save(review);
        return toResponse(review);
    }

    /** 피어리뷰 수정 (본인만) */
    @Transactional
    public PeerReviewDto.Response update(Long projectId, Long reviewId, User reviewer, PeerReviewDto.UpdateRequest req) {
        PeerReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "리뷰를 찾을 수 없습니다."));
        if (!review.getProject().getId().equals(projectId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!review.getReviewer().getId().equals(reviewer.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 리뷰만 수정할 수 있습니다.");
        }
        review.setScore(req.getScore());
        review.setComment(req.getComment());
        review.setAnonymous(req.isAnonymous());
        return toResponse(review);
    }

    /** 내가 쓴 리뷰 목록 */
    public List<PeerReviewDto.Response> myReviews(Long projectId, Long reviewerId) {
        return reviewRepository.findByProjectIdAndReviewerId(projectId, reviewerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** 프로젝트 전체 피어리뷰 요약 (피평가자별 평균) */
    public List<PeerReviewDto.Summary> summary(Long projectId) {
        List<Object[]> rows = reviewRepository.avgScoreByReviewee(projectId);

        // 멤버 이름 매핑
        Map<Long, String> nameMap = memberRepository.findByProjectId(projectId).stream()
                .collect(Collectors.toMap(
                        m -> m.getUser().getId(),
                        m -> m.getUser().getName()));

        return rows.stream().map(row -> {
            Long revieweeId = (Long) row[0];
            double avg = ((Number) row[1]).doubleValue();
            long count = ((Number) row[2]).longValue();

            List<PeerReviewDto.Response> reviews = reviewRepository
                    .findByProjectIdAndRevieweeId(projectId, revieweeId)
                    .stream().map(this::toResponse).collect(Collectors.toList());

            return PeerReviewDto.Summary.builder()
                    .revieweeId(revieweeId)
                    .revieweeName(nameMap.getOrDefault(revieweeId, "Unknown"))
                    .avgScore(Math.round(avg * 10.0) / 10.0)
                    .reviewCount(count)
                    .reviews(reviews)
                    .build();
        }).collect(Collectors.toList());
    }

    private PeerReviewDto.Response toResponse(PeerReview r) {
        return PeerReviewDto.Response.builder()
                .id(r.getId())
                .projectId(r.getProject().getId())
                .reviewerId(r.isAnonymous() ? null : r.getReviewer().getId())
                .reviewerName(r.isAnonymous() ? "익명" : r.getReviewer().getName())
                .revieweeId(r.getReviewee().getId())
                .revieweeName(r.getReviewee().getName())
                .score(r.getScore())
                .comment(r.getComment())
                .anonymous(r.isAnonymous())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
