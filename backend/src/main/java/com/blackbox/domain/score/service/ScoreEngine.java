package com.blackbox.domain.score.service;

import com.blackbox.domain.activity.entity.ActivityLog.EventType;
import com.blackbox.domain.activity.repository.ActivityLogRepository;
import com.blackbox.domain.project.entity.ProjectMember;
import com.blackbox.domain.project.repository.ProjectMemberRepository;
import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.score.dto.ScoreDto;
import com.blackbox.domain.score.entity.Alert;
import com.blackbox.domain.score.entity.ContributionScore;
import com.blackbox.domain.score.entity.ProjectAlertConfig;
import com.blackbox.domain.score.entity.ProjectWeight;
import com.blackbox.domain.score.repository.AlertRepository;
import com.blackbox.domain.score.repository.ContributionScoreRepository;
import com.blackbox.domain.score.repository.ProjectAlertConfigRepository;
import com.blackbox.domain.score.repository.ProjectWeightRepository;
import com.blackbox.domain.task.entity.Task;
import com.blackbox.domain.task.repository.TaskRepository;
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

    // 기본 가중치 (프로젝트별 project_weights 테이블로 오버라이드 가능)
    private static final double DEFAULT_W_TASK    = 0.35;
    private static final double DEFAULT_W_MEETING = 0.30;
    private static final double DEFAULT_W_FILE    = 0.20;
    private static final double DEFAULT_W_EXTRA   = 0.15;

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
            Map.entry(EventType.GFORM_RESPONSE_SUBMITTED,  3.0),
            // 수동 신고 (trust_level=0.7 이미 반영됨)
            Map.entry(EventType.MANUAL_WORK_REPORTED,      5.0)
    );

    private static final double NORMALIZED_CAP = 150.0;

    private final ActivityLogRepository activityLogRepository;
    private final ContributionScoreRepository scoreRepository;
    private final AlertRepository alertRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectWeightRepository weightRepository;
    private final ProjectAlertConfigRepository alertConfigRepository;
    private final TaskRepository taskRepository;
    private final DiscordNotificationService discordNotificationService;

    /**
     * 특정 프로젝트의 점수를 재계산하고 저장한다.
     */
    @Transactional
    public ScoreDto.ProjectScoreReport calculate(Long projectId) {
        log.info("Score calculation started for project {}", projectId);

        var project = (com.blackbox.domain.project.entity.Project) projectRepository.findById(projectId).orElseThrow();
        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);

        // 프로젝트별 가중치 (없으면 기본값 사용)
        ProjectWeight pw = weightRepository.findByProjectId(projectId).orElse(null);
        double wTask    = pw != null ? pw.getWTask()    : DEFAULT_W_TASK;
        double wMeeting = pw != null ? pw.getWMeeting() : DEFAULT_W_MEETING;
        double wFile    = pw != null ? pw.getWFile()    : DEFAULT_W_FILE;
        double wExtra   = pw != null ? pw.getWExtra()   : DEFAULT_W_EXTRA;

        // 1. 활동 로그 집계: {userId -> {EventType -> trustWeightedCount}}
        Map<Long, Map<EventType, Double>> eventCounts = aggregateEvents(projectId);

        // 2. 유저별 raw 점수 계산
        // [0]=task [1]=meeting [2]=file [3]=extra(합) [4]=github [5]=notion [6]=google
        Map<Long, double[]> rawScores = new HashMap<>();
        for (ProjectMember member : members) {
            Long userId = member.getUser().getId();
            Map<EventType, Double> counts = eventCounts.getOrDefault(userId, Collections.emptyMap());

            double taskRaw    = score(counts, EventType.TASK_CREATED, EventType.TASK_UPDATED, EventType.TASK_STATUS_CHANGED);
            double meetingRaw = score(counts, EventType.MEETING_CREATED, EventType.MEETING_CHECKIN);
            double fileRaw    = score(counts, EventType.FILE_UPLOADED);
            double githubRaw  = score(counts,
                                      EventType.GITHUB_PUSH, EventType.GITHUB_PR_OPENED,
                                      EventType.GITHUB_PR_MERGED, EventType.GITHUB_ISSUE_OPENED,
                                      EventType.GITHUB_ISSUE_CLOSED);
            double notionRaw  = score(counts,
                                      EventType.NOTION_PAGE_CREATED, EventType.NOTION_PAGE_EDITED,
                                      EventType.NOTION_COMMENT_ADDED);
            double googleRaw  = score(counts,
                                      EventType.GDRIVE_FILE_UPLOADED, EventType.GDRIVE_FILE_MODIFIED,
                                      EventType.GSHEET_EDITED, EventType.GFORM_RESPONSE_SUBMITTED);
            double extraRaw   = githubRaw + notionRaw + googleRaw;

            rawScores.put(userId, new double[]{taskRaw, meetingRaw, fileRaw, extraRaw, githubRaw, notionRaw, googleRaw});
        }

        // 3. 종합 점수 = Σ(항목 × 가중치)
        Map<Long, Double> totals = new HashMap<>();
        for (var entry : rawScores.entrySet()) {
            double[] r = entry.getValue();
            double total = r[0] * wTask + r[1] * wMeeting + r[2] * wFile + r[3] * wExtra;
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
        ProjectAlertConfig cfg = alertConfigRepository.findByProjectId(projectId).orElseGet(ProjectAlertConfig::new);
        runAlertEngine(projectId, (com.blackbox.domain.project.entity.Project) project, members, normalized, totals, eventCounts, cfg);

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
    @Transactional
    public ScoreDto.ProjectScoreReport getReport(Long projectId) {
        List<ContributionScore> scores = scoreRepository.findByProjectIdOrderByNormalizedScoreDesc(projectId);
        if (scores.isEmpty()) return calculate(projectId);

        // 외부활동 세부 분류는 DB에 저장되지 않으므로 이벤트 집계에서 재계산
        Map<Long, Map<EventType, Double>> eventCounts = aggregateEvents(projectId);

        List<ScoreDto.MemberScore> memberScores = scores.stream()
                .map(s -> {
                    Long uid = s.getUser().getId();
                    Map<EventType, Double> counts = eventCounts.getOrDefault(uid, Collections.emptyMap());
                    double githubScore = score(counts,
                            EventType.GITHUB_PUSH, EventType.GITHUB_PR_OPENED,
                            EventType.GITHUB_PR_MERGED, EventType.GITHUB_ISSUE_OPENED,
                            EventType.GITHUB_ISSUE_CLOSED);
                    double notionScore = score(counts,
                            EventType.NOTION_PAGE_CREATED, EventType.NOTION_PAGE_EDITED,
                            EventType.NOTION_COMMENT_ADDED);
                    double googleScore = score(counts,
                            EventType.GDRIVE_FILE_UPLOADED, EventType.GDRIVE_FILE_MODIFIED,
                            EventType.GSHEET_EDITED, EventType.GFORM_RESPONSE_SUBMITTED);
                    return ScoreDto.MemberScore.builder()
                            .userId(uid)
                            .userName(s.getUser().getName())
                            .taskScore(s.getTaskScore())
                            .meetingScore(s.getMeetingScore())
                            .fileScore(s.getFileScore())
                            .githubScore(githubScore)
                            .notionScore(notionScore)
                            .googleScore(googleScore)
                            .totalScore(s.getTotalScore())
                            .normalizedScore(s.getNormalizedScore())
                            .grade(toGrade(s.getNormalizedScore()))
                            .calculatedAt(s.getCalculatedAt())
                            .build();
                })
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
    private void runAlertEngine(Long projectId, com.blackbox.domain.project.entity.Project proj,
                                List<ProjectMember> members,
                                Map<Long, Double> normalized,
                                Map<Long, Double> totals,
                                Map<Long, Map<EventType, Double>> eventCounts,
                                ProjectAlertConfig cfg) {

        // 불균형 감지
        if (members.size() >= 2) {
            double max = normalized.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double min = normalized.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);
            if ((max - min) > cfg.getImbalancePct() && !alertExists(projectId, Alert.AlertType.IMBALANCE)) {
                saveAlert(proj, Alert.AlertType.IMBALANCE, "WARNING",
                        String.format("팀 내 기여도 편차가 %.1f%%p로 임계값(%.0f%%p)을 초과합니다.", max - min, cfg.getImbalancePct()), cfg);
            }
        }

        // 과부하 감지
        double totalSum = totals.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalSum > 0) {
            totals.forEach((userId, total) -> {
                double ratio = total / totalSum;
                if (ratio >= cfg.getOverloadRatio() && !alertExists(projectId, Alert.AlertType.OVERLOAD)) {
                    saveAlert(proj, Alert.AlertType.OVERLOAD, "CRITICAL",
                            String.format("한 멤버가 팀 전체 활동의 %.0f%%를 담당하고 있습니다.", ratio * 100), cfg);
                }
            });
        }

        // 이탈 감지
        Instant inactivitySince = Instant.now().minus(cfg.getInactivityDays(), ChronoUnit.DAYS);
        for (ProjectMember member : members) {
            Long userId = member.getUser().getId();
            boolean active = activityLogRepository.existsByProjectIdAndUserIdAndCreatedAtAfter(
                    projectId, userId, inactivitySince);
            if (!active && !alertExists(projectId, Alert.AlertType.INACTIVITY)) {
                saveAlert(proj, Alert.AlertType.INACTIVITY, "WARNING",
                        String.format("멤버 '%s'가 %d일 이상 활동이 없습니다.", member.getUser().getName(), cfg.getInactivityDays()), cfg);
            }
        }

        // 벼락치기 감지: 최근 N일 활동이 전체의 crammingRatio 이상
        Instant crammingSince = Instant.now().minus(cfg.getCrammingDays(), ChronoUnit.DAYS);
        for (ProjectMember member : members) {
            Long userId = member.getUser().getId();
            long total  = activityLogRepository.countByProjectIdAndUserId(projectId, userId);
            if (total < cfg.getMinEvents()) continue;
            long recent = activityLogRepository.countByProjectIdAndUserIdAndCreatedAtAfter(projectId, userId, crammingSince);
            double ratio = (double) recent / total;
            if (ratio >= cfg.getCrammingRatio() && !alertExists(projectId, Alert.AlertType.CRAMMING)) {
                saveAlert(proj, Alert.AlertType.CRAMMING, "WARNING",
                        String.format("멤버 '%s'의 활동 중 %.0f%%가 최근 %d일에 집중되어 있습니다.",
                                member.getUser().getName(), ratio * 100, cfg.getCrammingDays()), cfg);
            }
        }

        // 마감 임박 감지: deadlineDays 이내에 마감되는 미완료 태스크
        Instant now = Instant.now();
        Instant deadlineCutoff = now.plus(cfg.getDeadlineDays(), ChronoUnit.DAYS);
        List<Task> urgentTasks = taskRepository.findByProjectIdAndStatusNotAndDueDateBetween(
                projectId, Task.Status.DONE, now, deadlineCutoff);
        if (!urgentTasks.isEmpty() && !alertExists(projectId, Alert.AlertType.DEADLINE)) {
            saveAlert(proj, Alert.AlertType.DEADLINE, "WARNING",
                    String.format("마감 %d일 이내인 미완료 태스크가 %d개 있습니다.",
                            cfg.getDeadlineDays(), urgentTasks.size()), cfg);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────────────────────
    private Map<Long, Map<EventType, Double>> aggregateEvents(Long projectId) {
        List<Object[]> rows = activityLogRepository.countByProjectGroupByUserAndType(projectId);
        Map<Long, Map<EventType, Double>> result = new HashMap<>();
        for (Object[] row : rows) {
            // native query → user_id: Number, event_type: String, sum: Number
            Long userId = ((Number) row[0]).longValue();
            EventType type;
            try {
                type = EventType.valueOf((String) row[1]);
            } catch (IllegalArgumentException e) {
                continue; // 알 수 없는 이벤트 타입은 무시
            }
            double weightedCount = ((Number) row[2]).doubleValue();
            result.computeIfAbsent(userId, k -> new HashMap<>()).put(type, weightedCount);
        }
        return result;
    }

    private double score(Map<EventType, Double> counts, EventType... types) {
        double sum = 0;
        for (EventType t : types) {
            sum += counts.getOrDefault(t, 0.0) * EVENT_POINTS.getOrDefault(t, 0.0);
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

    private void saveAlert(com.blackbox.domain.project.entity.Project project,
                           Alert.AlertType type, String severity, String message,
                           ProjectAlertConfig cfg) {
        saveAlert(project, type, severity, message);
        discordNotificationService.sendAlert(
                cfg.getDiscordWebhookUrl(), project.getName(), type, severity, message);
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
                    double[] r = rawScores.getOrDefault(uid, new double[7]);
                    double norm = normalized.getOrDefault(uid, 0.0);
                    return ScoreDto.MemberScore.builder()
                            .userId(uid)
                            .userName(m.getUser().getName())
                            .taskScore(r[0])
                            .meetingScore(r[1])
                            .fileScore(r[2])
                            .githubScore(r.length > 4 ? r[4] : 0)
                            .notionScore(r.length > 5 ? r[5] : 0)
                            .googleScore(r.length > 6 ? r[6] : 0)
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
