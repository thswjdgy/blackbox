package com.blackbox.domain.score.entity;

import com.blackbox.domain.project.entity.Project;
import com.blackbox.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "weight_history")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class WeightHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(nullable = false) private double wTask;
    @Column(nullable = false) private double wMeeting;
    @Column(nullable = false) private double wFile;
    @Column(nullable = false) private double wExtra;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant changedAt;
}
