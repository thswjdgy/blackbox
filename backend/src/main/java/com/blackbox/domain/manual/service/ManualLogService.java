package com.blackbox.domain.manual.service;

import com.blackbox.domain.activity.entity.ActivityLog;
import com.blackbox.domain.activity.service.ActivityLogService;
import com.blackbox.domain.manual.dto.ManualLogDto;
import com.blackbox.domain.manual.entity.ManualLog;
import com.blackbox.domain.manual.repository.ManualLogRepository;
import com.blackbox.domain.project.entity.Project;
import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.global.exception.BusinessException;
import com.blackbox.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManualLogService {

    private final ManualLogRepository manualLogRepository;
    private final ProjectRepository projectRepository;
    private final ActivityLogService activityLogService;

    /** 수동 작업 신고 제출 */
    @Transactional
    public ManualLogDto.Response submit(Long projectId, User user, ManualLogDto.CreateRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        ManualLog log = ManualLog.builder()
                .project(project)
                .user(user)
                .title(req.getTitle())
                .description(req.getDescription())
                .workDate(req.getWorkDate())
                .evidenceUrl(req.getEvidenceUrl())
                .build();
        manualLogRepository.save(log);

        // activity_log 에 PENDING 상태로 기록 (trust_level 0.7)
        activityLogService.log(project, user, null, null,
                ActivityLog.EventType.MANUAL_WORK_REPORTED,
                "PLATFORM", 0.7,
                Map.of("title", req.getTitle(), "manualLogId", log.getId().toString()));

        return toResponse(log);
    }

    /** 프로젝트 내 수동 신고 목록 */
    public List<ManualLogDto.Response> list(Long projectId) {
        return manualLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** 내 수동 신고 목록 */
    public List<ManualLogDto.Response> myList(Long projectId, Long userId) {
        return manualLogRepository.findByProjectIdAndUserIdOrderByCreatedAtDesc(projectId, userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** 팀장 승인/거절 */
    @Transactional
    public ManualLogDto.Response review(Long projectId, Long logId, User reviewer, ManualLogDto.ReviewRequest req) {
        ManualLog log = manualLogRepository.findById(logId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));
        if (!log.getProject().getId().equals(projectId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        log.setStatus(req.getStatus().toUpperCase());
        log.setReviewedBy(reviewer);
        log.setReviewNote(req.getReviewNote());
        log.setReviewedAt(Instant.now());
        return toResponse(log);
    }

    private ManualLogDto.Response toResponse(ManualLog l) {
        return ManualLogDto.Response.builder()
                .id(l.getId())
                .projectId(l.getProject().getId())
                .userId(l.getUser().getId())
                .userName(l.getUser().getName())
                .title(l.getTitle())
                .description(l.getDescription())
                .workDate(l.getWorkDate())
                .evidenceUrl(l.getEvidenceUrl())
                .trustLevel(l.getTrustLevel())
                .status(l.getStatus())
                .reviewedById(l.getReviewedBy() != null ? l.getReviewedBy().getId() : null)
                .reviewNote(l.getReviewNote())
                .reviewedAt(l.getReviewedAt())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
