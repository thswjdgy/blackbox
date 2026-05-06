package com.blackbox.domain.review.controller;

import com.blackbox.domain.review.dto.PeerReviewDto;
import com.blackbox.domain.review.service.PeerReviewService;
import com.blackbox.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/peer-reviews")
@RequiredArgsConstructor
public class PeerReviewController {

    private final PeerReviewService peerReviewService;

    /** POST /api/projects/{projectId}/peer-reviews */
    @PostMapping
    public ResponseEntity<PeerReviewDto.Response> submit(
            @PathVariable Long projectId,
            @Valid @RequestBody PeerReviewDto.CreateRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(peerReviewService.submit(projectId, user, req));
    }

    /** PUT /api/projects/{projectId}/peer-reviews/{reviewId} — 리뷰 수정 */
    @PutMapping("/{reviewId}")
    public ResponseEntity<PeerReviewDto.Response> update(
            @PathVariable Long projectId,
            @PathVariable Long reviewId,
            @RequestBody PeerReviewDto.UpdateRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(peerReviewService.update(projectId, reviewId, user, req));
    }

    /** GET /api/projects/{projectId}/peer-reviews/me — 내가 쓴 리뷰 */
    @GetMapping("/me")
    public ResponseEntity<List<PeerReviewDto.Response>> myReviews(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(peerReviewService.myReviews(projectId, user.getId()));
    }

    /** GET /api/projects/{projectId}/peer-reviews/summary — 전체 요약 */
    @GetMapping("/summary")
    public ResponseEntity<List<PeerReviewDto.Summary>> summary(@PathVariable Long projectId) {
        return ResponseEntity.ok(peerReviewService.summary(projectId));
    }
}
