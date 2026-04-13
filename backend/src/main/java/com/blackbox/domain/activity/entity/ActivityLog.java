package com.blackbox.domain.activity.entity;

import com.blackbox.domain.project.entity.Project;
import com.blackbox.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "task_id")
    private Long taskId; // Storing as Long instead of ManyToOne to avoid circular deps with Task

    @Column(name = "meeting_id")
    private Long meetingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventType eventType;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String source = "PLATFORM";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public enum EventType {
        TASK_CREATED,
        TASK_UPDATED,
        TASK_STATUS_CHANGED,
        TASK_DELETED,
        MEETING_CREATED,
        MEETING_CHECKIN,
        MEMBER_JOINED,
        FILE_UPLOADED,
        FILE_TAMPERED,
        // GitHub 연동
        GITHUB_PUSH,
        GITHUB_PR_OPENED,
        GITHUB_PR_MERGED,
        GITHUB_ISSUE_OPENED,
        GITHUB_ISSUE_CLOSED,
        // Notion 연동
        NOTION_PAGE_CREATED,
        NOTION_PAGE_EDITED,
        NOTION_COMMENT_ADDED,
        // Google 연동
        GDRIVE_FILE_UPLOADED,
        GDRIVE_FILE_MODIFIED,
        GSHEET_EDITED,
        GFORM_RESPONSE_SUBMITTED
    }
}
