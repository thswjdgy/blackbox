package com.blackbox.domain.vault.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tamper_detection_log")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TamperDetectionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vault_id", nullable = false)
    private FileVault vault;

    @Column(name = "expected_hash", nullable = false, length = 64)
    private String expectedHash;

    @Column(name = "actual_hash", nullable = false, length = 64)
    private String actualHash;

    @CreationTimestamp
    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;
}
