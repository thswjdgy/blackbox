package com.blackbox.domain.score.service;

import com.blackbox.domain.activity.entity.ActivityLog.EventType;
import com.blackbox.domain.activity.repository.ActivityLogRepository;
import com.blackbox.domain.project.entity.ProjectMember;
import com.blackbox.domain.project.repository.ProjectMemberRepository;
import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.score.dto.ScoreDto;
import com.blackbox.domain.score.entity.Alert;
import com.blackbox.domain.score.entity.ContributionScore;
import com.blackbox.domain.score.repository.AlertRepository;
import com.blackbox.domain.score.repository.ContributionScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreEngine {

    // 가중치 (w1~w4, 합계 = 1.0)
    private static final double W_TASK    = 0.35;
    private static final double W_MEETING = 0.30;
    private static final double W_FILE    = 0.20;
    private static final double W_EXTRA   = 0.15; // 액션 아이템, 기타 활동

    // 이벤트별 포인트
    private static final Map<EventType, Double> EVENT_POINTS = Map.ofEntries(
            Map.entry(EventType.TASK_CREATED,        3.0),
            Map.entry(EventType.TASK_UPDATED,        1.0),
            Map.entry(EventType.TASK_STATUS_CHANGED, 4.0),
            Map.entry(EventType.MEETING_CREATED,     5.0),
            Map.entry(EventType.MEETING_CHECKIN,     8.0),
            Map.entry(EventType.FILE_UPLOADED,       6.0),
            Map.entry(EventType.FILE_TAMPERED,       0.0),
            // GitHub 활동 포인트
            Map.entry(EventType.GITHUB_PUSH,         3.0),
            Map.entry(EventType.GITHUB_PR_OPENED,    4.0),
            Map.entry(EventType.GITHUB_PR_MERGED,    6.0),
            Map.entry(EventType.GITHUB_ISSUE_OPENED, 2.0),
            Map.entry(EventType.GITHUB_ISSUE_CLOSED, 3.0),
            // Notion 활동 포인트
            Map.entry(EventType.NOTION_PAGE_CREATED, 4.0),
            Map.entry(EventType.NOTION_PAGE_EDITED,  2.0),
            Map.entry(EventType.NOTION_COMMENT_ADDED,1.0),
            // Google 활동 포인트
            Map.entry(EventType.GDRIVE_FILE_UPLOADED,      5.0),
            Map.entry(EventType.GDRIVE_FILE_MODIFIED,      2.0),
            Map.entry(EventType.GSHEET_EDITED,             2.0),
            Map.entry(EventType.GFORM_RESPONSE_SUBMITTED,  3.0)
    );

    private static final double NORMALIZED_CAP = 150.0;
    private static final double INACTIVITY_DAYS = 14.0;

    private final ActivityLogRepository activityLogRepository;
    private final ContributionScoreRepository scoreRepository;
    private final AlertRepository alertRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    /**
     * 특정 프로젝트의 점수를 재계산하고 저장한다.
     */
    @Transactional
    public ScoreDto.ProjectScoreReport calculate(Long projectId) {
        log.info("Score calculation started for project {}", projectId);

        var project = projectRepository.findById(projectId).orElseThrow();
        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);

        // 1. 활동 로그 집계: {userId -> {EventType -> count}}
        Map<Long, Map<EventType, Long>> eventCounts = aggregateEvents(projectId);

        // 2. 유저별 raw 점수 계산
        Map<Long, double[]> rawScores = new HashMap<>(); // [task, meeting, file, extra]
        for (ProjectMember member : members) {
            Long userId = member.getUser().getId();
            Map<EventType, Long> counts = eventCounts.getOrDefault(userId, Collections.emptyMap());

            double taskRaw    = score(counts, EventType.TASK_CREATED, EventType.TASK_UPDATED, EventType.TASK_STATUS_CHANGED);
            double meetingRaw = score(counts, EventType.MEETING_CREATED, EventType.MEETING_CHECKIN);
            double fileRaw    = score(counts, EventType.FILE_UPLOADED);
            double extraRaw   = score(counts,
                                      EventType.GITHUB_PUSH, EventType.GITHUB_PR_OPENED,
                                      EventType.GITHUB_PR_MERGED, EventType.GITHUB_ISSUE_OPENED,
                                      EventType.GITHUB_ISSUE_CLOSED,
                                      EventType.NOTION_PAGE_CREATED, EventType.NOTION_PAGE_EDITED,
                                      EventType.NOTION_COMMENT_ADDED,
                                      EventType.GDRIVE_FILE_UPLOADED, EventType.GDRIVE_FILE_MODIFIED,
                                      EventType.GSHEET_EDITED, EventType.GFORM_RESPONSE_SUBMITTED);

            rawScores.put(userId, new double[]{taskRaw, meetingRaw, fileRaw, extraRaw});
        }

        // 3. 종합 점수 = Σ(항목 × 가중치)
        Map<Long, Double> totals = new HashMap<>();
        for (var entry : rawScores.entrySet()) {
            double[] r = entry.getValue();
            double total = r[0] * W_TASK + r[1] * W_MEETING + r[2] * W_FILE + r[3] * W_EXTRA;
            totals.put(entry.getKey(), total);
        }

        // 4. 팀 평균 기반 정규화 (상한 150%)
        double teamAvg = totals.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        Map<Long, Double> normalized = new HashMap<>();
        for (var entry : totals.entrySet()) {
            double norm = teamAvg > 0
                    ? Math.min((entry.getValue() / teamAvg) * 100.0, NORMALIZED_CAP)
                    : 0;
            normalized.put(entry.getKey(), norm);
        }

        // 5. DB 저장 (UPSERT)
        Instant now = Instant.now();
        for (ProjectMember member : members) {
            Long userId = member.getUser().getId();
            double[] r = rawScores.getOrDefault(userId, new double[4]);

            ContributionScore score = scoreRepository
                    .findByProjectIdAndUserId(projectId, userId)
                    .orElse(ContributionScore.builder().project(project).user(member.getUser()).build());

            score.setTaskScore(r[0]);
            score.setMeetingScore(r[1]);
            score.setFileScore(r[2]);
            score.setTotalScore(totals.getOrDefault(userId, 0.0));
            score.setNormalizedScore(normalized.getOrDefault(userId, 0.0));
            score.setCalculatedAt(now);
            scoreRepository.save(score);
        }

        // 6. 경보 분석
        runAlertEngine(projectId, project, members, normalized, totals, eventCounts);

        log.info("Score calculation done for project {}", projectId);
        return buildReport(projectId, members, rawScores, totals, normalized, teamAvg, now);
    }

    /**
     * 모든 활성 프로젝트 점수 재계산 (스케줄러용)
     */
    @Transactional
    public void calculateAll() {
        projectRepository.findAll().stream()
                .filter(p -> p.isActive())
                .forEach(p -> {
                    try { calculate(p.getId()); }
                    catch (Exception e) { log.error("Score calc failed for project {}: {}", p.getId(), e.getMessage()); }
                });
    }

    /** 저장된 점수 조회 */
    @Transactional(readOnly = true)
    public ScoreDto.ProjectScoreReport getReport(Long projectId) {
        List<ContributionScore> scores = scoreRepository.findByProjectIdOrderByNormalizedScoreDesc(projectId);
        if (scores.isEmpty()) return calculate(projectId);

        List<ScoreDto.MemberScore> memberScores = scores.stream()
                .map(s -> ScoreDto.MemberScore.builder()
                        .userId(s.getUser().getId())
                        .userName(s.getUser().getName())
                        .taskScore(s.getTaskScore())
                        .meetingScore(s.getMeetingScore())
                        .fileScore(s.getFileScore())
                        .totalScore(s.getTotalScore())
                        .normalizedScore(s.getNormalizedScore())
                        .grade(toGrade(s.getNormalizedScore()))
                        .calculatedAt(s.getCalculatedAt())
                        .build())
                .collect(Collectors.toList());

        double avg = scores.stream().mapToDouble(ContributionScore::getTotalScore).average().orElse(0);
        return ScoreDto.ProjectScoreReport.builder()
                .projectId(projectId)
                .members(memberScores)
                .teamAverage(avg)
                .calculatedAt(scores.get(0).getCalculatedAt())
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────
    // 경보 엔진
    // ──────────────────────────────────────────────────────────────────────
    private void runAlertEngine(Long projectId, Object project,
                                List<ProjectMember> members,
                                Map<Long, Double> normalized,
                                Map<Long, Double> totals,
                                Map<Long, Map<EventType, Long>> eventCounts) {
        var proj = (com.blackbox.domain.project.entity.Project) project;

        // 불균형 감지: max - min > 40%p
        if (members.size() >= 2) {
            double max = normalized.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double min = normalized.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);
            if ((max - min) > 40.0 && !alertExists(projectId, Alert.AlertType.IMBALANCE)) {
                saveAlert(proj, Alert.AlertType.IMBALANCE, "WARNING",
                        String.format("팀 내 기여도 편차가 %.1f%%p 로 40%%p를 초과합니다.", max - min));
            }
        }

        // 과부하 감지: 1인 총점이 팀 전체의 60% 이상
        double totalSum = totals.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalSum > 0) {
            totals.forEach((userId, total) -> {
                double ratio = total / totalSum;
                if (ratio >= 0.6 && !alertExists(projectId, Alert.AlertType.OVERLOAD)) {
                    saveAlert(proj, Alert.AlertType.OVERLOAD, "CRITICAL",
                            String.format("한 멤버가 팀 전체 활동의 %.0f%%를 담당하고 있습니다.", ratio * 100));
                }
            });
        }

        // 이탈 감지: 14일 이상 무활동
        Instant twoWeeksAgo = Instant.now().minus((long) INACTIVITY_DAYS, ChronoUnit.DAYS);
        for (ProjectMember member : members) {
            Long userId = member.getUser().getId();
            boolean active = activityLogRepository.existsByProjectIdAndUserIdAndCreatedAtAfter(
                    projectId, userId, twoWeeksAgo);
            if (!active && !alertExists(projectId, Alert.AlertType.INACTIVITY)) {
                saveAlert(proj, Alert.AlertType.INACTIVITY, "WARNING",
                        String.format("멤버 '%s'가 14일 이상 활동이 없습니다.", member.getUser().getName()));
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────────────────────
    private Map<Long, Map<EventType, Long>> aggregateEvents(Long projectId) {
        List<Object[]> rows = activityLogRepository.countByProjectGroupByUserAndType(projectId);
        Map<Long, Map<EventType, Long>> result = new HashMap<>();
        for (Object[] row : rows) {
            Long userId = (Long) row[0];
            EventType type = (EventType) row[1];
            Long count = (Long) row[2];
            result.computeIfAbsent(userId, k -> new HashMap<>()).put(type, count);
        }
        return result;
    }

    private double score(Map<EventType, Long> counts, EventType... types) {
        double sum = 0;
        for (EventType t : types) {
            sum += counts.getOrDefault(t, 0L) * EVENT_POINTS.getOrDefault(t, 0.0);
        }
        return sum;
    }

    private boolean alertExists(Long projectId, Alert.AlertType type) {
        return alertRepository.existsByProjectIdAndAlertTypeAndIsResolvedFalse(projectId, type);
    }

    private void saveAlert(com.blackbox.domain.project.entity.Project project,
                           Alert.AlertType type, String severity, String message) {
        alertRepository.save(Alert.builder()
                .project(project).alertType(type).severity(severity).message(message).build());
        log.warn("Alert created: {} for project {}", type, project.getId());
    }

    private ScoreDto.ProjectScoreReport buildReport(Long projectId,
                                                    List<ProjectMember> members,
                                                    Map<Long, double[]> rawScores,
                                                    Map<Long, Double> totals,
                                                    Map<Long, Double> normalized,
                                                    double teamAvg, Instant now) {
        List<ScoreDto.MemberScore> memberScores = members.stream()
                .map(m -> {
                    Long uid = m.getUser().getId();
                    double[] r = rawScores.getOrDefault(uid, new double[4]);
                    double norm = normalized.getOrDefault(uid, 0.0);
                    return ScoreDto.MemberScore.builder()
                            .userId(uid)
                            .userName(m.getUser().getName())
                            .taskScore(r[0])
                            .meetingScore(r[1])
                            .fileScore(r[2])
                            .totalScore(totals.getOrDefault(uid, 0.0))
                            .normalizedScore(norm)
                            .grade(toGrade(norm))
                            .calculatedAt(now)
                            .build();
                })
                .sorted(Comparator.comparingDouble(ScoreDto.MemberScore::getNormalizedScore).reversed())
                .collect(Collectors.toList());

        return ScoreDto.ProjectScoreReport.builder()
                .projectId(projectId)
                .members(memberScores)
                .teamAverage(teamAvg)
                .calculatedAt(now)
                .build();
    }

    private String toGrade(double normalized) {
        if (normalized >= 120) return "A";
        if (normalized >= 100) return "B";
        if (normalized >= 80)  return "C";
        if (normalized >= 60)  return "D";
        return "F";
    }
}
