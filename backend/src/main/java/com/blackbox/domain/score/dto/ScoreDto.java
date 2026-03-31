package com.blackbox.domain.score.dto;

import com.blackbox.domain.score.entity.Alert;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

public class ScoreDto {

    @Getter
    @Builder
    public static class MemberScore {
        private Long userId;
        private String userName;
        private double taskScore;
        private double meetingScore;
        private double fileScore;
        private double totalScore;
        private double normalizedScore;  // 100 기준, 150 상한
        private String grade;            // A/B/C/D/F
        private Instant calculatedAt;
    }

    @Getter
    @Builder
    public static class ProjectScoreReport {
        private Long projectId;
        private List<MemberScore> members;
        private double teamAverage;
        private Instant calculatedAt;
    }

    @Getter
    @Builder
    public static class AlertResponse {
        private Long id;
        private Long projectId;
        private Alert.AlertType alertType;
        private String severity;
        private String message;
        private boolean resolved;
        private Instant createdAt;
        private Instant resolvedAt;
    }
}
