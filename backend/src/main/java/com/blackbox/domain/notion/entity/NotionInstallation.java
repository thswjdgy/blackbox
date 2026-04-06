package com.blackbox.domain.notion.entity;

import com.blackbox.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notion_installations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotionInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "integration_token", nullable = false, length = 512)
    private String integrationToken;

    /** 특정 Notion Database ID (null이면 전체 워크스페이스 검색) */
    @Column(name = "database_id", length = 255)
    private String databaseId;

    @Column(name = "workspace_name", length = 255)
    private String workspaceName;

    @Column(name = "last_polled_at")
    private Instant lastPolledAt;

    @CreationTimestamp
    @Column(name = "connected_at", nullable = false, updatable = false)
    private Instant connectedAt;
}
