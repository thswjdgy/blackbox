package com.blackbox.domain.score.entity;

import com.blackbox.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_alert_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAlertConfig {

    @Id
    @Column(name = "project_id")
    private Long projectId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "imbalance_pct", nullable = false)
    @Builder.Default
    private double imbalancePct = 40.0;

    @Column(name = "overload_ratio", nullable = false)
    @Builder.Default
    private double overloadRatio = 0.60;

    @Column(name = "inactivity_days", nullable = false)
    @Builder.Default
    private int inactivityDays = 14;

    @Column(name = "cramming_days", nullable = false)
    @Builder.Default
    private int crammingDays = 7;

    @Column(name = "cramming_ratio", nullable = false)
    @Builder.Default
    private double crammingRatio = 0.60;

    @Column(name = "min_events", nullable = false)
    @Builder.Default
    private int minEvents = 10;
}
