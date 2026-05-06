package com.blackbox.domain.activity.service;

import com.blackbox.domain.activity.entity.ActivityLog;
import com.blackbox.domain.activity.entity.ActivityLog.EventType;
import com.blackbox.domain.activity.repository.ActivityLogRepository;
import com.blackbox.domain.project.entity.Project;
import com.blackbox.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Transactional
    public void logTaskEvent(Project project, User user, Long taskId, EventType type, Map<String, Object> payload) {
        ActivityLog logentry = ActivityLog.builder()
                .project(project)
                .user(user)
                .taskId(taskId)
                .eventType(type)
                .payload(payload)
                .source("PLATFORM")
                .build();
        activityLogRepository.save(logentry);
        log.info("Logged Task Event: {} for Project: {}", type, project.getId());
    }

    @Transactional
    public void logVaultEvent(Project project, User user, EventType type, Map<String, Object> payload) {
        ActivityLog logentry = ActivityLog.builder()
                .project(project)
                .user(user)
                .eventType(type)
                .payload(payload)
                .source("PLATFORM")
                .build();
        activityLogRepository.save(logentry);
        log.info("Logged Vault Event: {} for Project: {}", type, project.getId());
    }

    @Transactional
    public void logMeetingEvent(Project project, User user, Long meetingId, EventType type, Map<String, Object> payload) {
        ActivityLog logentry = ActivityLog.builder()
                .project(project)
                .user(user)
                .meetingId(meetingId)
                .eventType(type)
                .payload(payload)
                .source("PLATFORM")
                .build();
        activityLogRepository.save(logentry);
        log.info("Logged Meeting Event: {} for Project: {}", type, project.getId());
    }

    /** 신뢰도 가중치 지정 가능한 범용 로그 */
    @Transactional
    public void log(Project project, User user, Long taskId, Long meetingId,
                    EventType type, String source, double trustLevel, Map<String, Object> payload) {
        ActivityLog logentry = ActivityLog.builder()
                .project(project)
                .user(user)
                .taskId(taskId)
                .meetingId(meetingId)
                .eventType(type)
                .source(source)
                .trustLevel(trustLevel)
                .payload(payload)
                .build();
        activityLogRepository.save(logentry);
        log.info("Logged {} (trust={}) for Project: {}", type, trustLevel, project.getId());
    }
}
