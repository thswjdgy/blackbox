package com.blackbox.domain.github.entity;

import com.blackbox.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "github_installations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @Column(name = "repo_full_name", nullable = false)
    private String repoFullName; // "owner/repo"

    @Column(name = "github_token")
    private String githubToken; // PAT for polling

    @Column(name = "webhook_secret")
    private String webhookSecret;

    @Column(name = "last_polled_at")
    private Instant lastPolledAt;

    @CreationTimestamp
    @Column(name = "connected_at", nullable = false, updatable = false)
    private Instant connectedAt;
}
