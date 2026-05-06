package com.blackbox.domain.score.entity;

import com.blackbox.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "project_weights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectWeight {

    @Id
    @Column(name = "project_id")
    private Long projectId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "w_task", nullable = false, columnDefinition = "NUMERIC(5,4)")
    @Builder.Default
    private double wTask = 0.35;

    @Column(name = "w_meeting", nullable = false, columnDefinition = "NUMERIC(5,4)")
    @Builder.Default
    private double wMeeting = 0.30;

    @Column(name = "w_file", nullable = false, columnDefinition = "NUMERIC(5,4)")
    @Builder.Default
    private double wFile = 0.20;

    @Column(name = "w_extra", nullable = false, columnDefinition = "NUMERIC(5,4)")
    @Builder.Default
    private double wExtra = 0.15;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
