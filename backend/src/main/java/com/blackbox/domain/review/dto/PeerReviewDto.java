package com.blackbox.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

public class PeerReviewDto {

    @Getter
    public static class CreateRequest {
        @NotNull
        private Long revieweeId;
        @Min(1) @Max(5)
        private int score;
        private String comment;
        private boolean anonymous = true;
    }

    @Getter
    public static class UpdateRequest {
        @Min(1) @Max(5)
        private int score;
        private String comment;
        private boolean anonymous = true;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long projectId;
        private Long reviewerId;   // anonymous=true면 null 반환
        private String reviewerName;
        private Long revieweeId;
        private String revieweeName;
        private int score;
        private String comment;
        private boolean anonymous;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Getter
    @Builder
    public static class Summary {
        private Long revieweeId;
        private String revieweeName;
        private double avgScore;
        private long reviewCount;
        private List<Response> reviews;
    }
}
