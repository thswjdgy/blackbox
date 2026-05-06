package com.blackbox.domain.project.entity;

import com.blackbox.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "project_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProjectRole projectRole = ProjectRole.MEMBER;

    @Column(nullable = false)
    @Builder.Default
    private boolean dataCollectionConsent = false;

    @Column
    private Instant consentAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean consentGithub = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean consentDrive = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean consentAi = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    public enum ProjectRole {
        LEADER, MEMBER, OBSERVER
    }
}
