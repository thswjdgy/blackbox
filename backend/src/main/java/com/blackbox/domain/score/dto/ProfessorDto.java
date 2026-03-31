package com.blackbox.domain.score.dto;

import lombok.Builder;
import lombok.Getter;

public class ProfessorDto {

    @Getter
    @Builder
    public static class ProjectOverviewResponse {
        private Long projectId;
        private String projectName;
        private int memberCount;
        private double averageScore;
        private int alertCount;        // e.g. Free-riding detected
        private int completedTasks;
        private int totalTasks;
    }
}
