package com.blackbox.domain.manual.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

public class ManualLogDto {

    @Getter
    public static class CreateRequest {
        @NotBlank
        private String title;
        private String description;
        @NotNull
        private LocalDate workDate;
        private String evidenceUrl;
    }

    @Getter
    public static class ReviewRequest {
        @NotBlank
        private String status; // APPROVED / REJECTED
        private String reviewNote;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long projectId;
        private Long userId;
        private String userName;
        private String title;
        private String description;
        private LocalDate workDate;
        private String evidenceUrl;
        private double trustLevel;
        private String status;
        private Long reviewedById;
        private String reviewNote;
        private Instant reviewedAt;
        private Instant createdAt;
    }
}
