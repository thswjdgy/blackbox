package com.blackbox.domain.google.entity;

import com.blackbox.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "google_installations")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GoogleInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT", nullable = false)
    private String refreshToken;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    /** 특정 Drive 폴더 ID (null이면 전체) */
    @Column(name = "drive_folder_id", length = 255)
    private String driveFolderId;

    /** 특정 Spreadsheet ID */
    @Column(name = "sheet_id", length = 255)
    private String sheetId;

    /** 특정 Form ID */
    @Column(name = "form_id", length = 255)
    private String formId;

    @Column(name = "drive_polled_at")
    private Instant drivePolledAt;

    @Column(name = "sheets_polled_at")
    private Instant sheetsPolledAt;

    @Column(name = "forms_polled_at")
    private Instant formsPolledAt;

    @CreationTimestamp
    @Column(name = "connected_at", nullable = false, updatable = false)
    private Instant connectedAt;
}
