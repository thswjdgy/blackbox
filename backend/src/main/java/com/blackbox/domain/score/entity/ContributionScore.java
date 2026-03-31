package com.blackbox.domain.score.entity;

import com.blackbox.domain.project.entity.Project;
import com.blackbox.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "contribution_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "task_score", nullable = false)
    @Builder.Default
    private double taskScore = 0;

    @Column(name = "meeting_score", nullable = false)
    @Builder.Default
    private double meetingScore = 0;

    @Column(name = "file_score", nullable = false)
    @Builder.Default
    private double fileScore = 0;

    @Column(name = "total_score", nullable = false)
    @Builder.Default
    private double totalScore = 0;

    @Column(name = "normalized_score", nullable = false)
    @Builder.Default
    private double normalizedScore = 0;

    @Column(name = "calculated_at", nullable = false)
    @Builder.Default
    private Instant calculatedAt = Instant.now();
}
