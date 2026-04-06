package com.blackbox.domain.activity.controller;

import com.blackbox.domain.activity.entity.ActivityLog;
import com.blackbox.domain.activity.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects/{projectId}/activities")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogRepository activityLogRepository;

    /**
     * 프로젝트 활동 타임라인
     *
     * @param source   ALL | PLATFORM | GITHUB (default: ALL)
     * @param userId   특정 멤버만 (optional)
     * @param limit    최대 반환 건수 (default: 50, max: 200)
     */
    @GetMapping
    public ResponseEntity<List<ActivityLogResponse>> getTimeline(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "ALL") String source,
            @RequestParam(required = false)     Long   userId,
            @RequestParam(defaultValue = "50")  int    limit) {

        limit = Math.min(limit, 200);
        var pageable = PageRequest.of(0, limit);

        List<ActivityLog> logs;
        boolean allSource = "ALL".equalsIgnoreCase(source);

        if (allSource && userId == null) {
            logs = activityLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
        } else if (allSource) {
            logs = activityLogRepository.findByProjectIdAndUserIdOrderByCreatedAtDesc(projectId, userId, pageable);
        } else if (userId == null) {
            logs = activityLogRepository.findByProjectIdAndSourceOrderByCreatedAtDesc(projectId, source.toUpperCase(), pageable);
        } else {
            logs = activityLogRepository.findByProjectIdAndSourceAndUserIdOrderByCreatedAtDesc(projectId, source.toUpperCase(), userId, pageable);
        }

        return ResponseEntity.ok(logs.stream().map(ActivityLogResponse::from).toList());
    }

    public record ActivityLogResponse(
            Long              id,
            Long              userId,
            String            userName,
            String            eventType,
            String            source,
            Map<String, Object> payload,
            Instant           createdAt
    ) {
        static ActivityLogResponse from(ActivityLog log) {
            return new ActivityLogResponse(
                    log.getId(),
                    log.getUser().getId(),
                    log.getUser().getName(),
                    log.getEventType().name(),
                    log.getSource(),
                    log.getPayload(),
                    log.getCreatedAt()
            );
        }
    }
}
