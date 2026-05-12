package com.blackbox.domain.ai.service;

import com.blackbox.domain.activity.entity.ActivityLog;
import com.blackbox.domain.activity.repository.ActivityLogRepository;
import com.blackbox.domain.project.entity.ProjectMember;
import com.blackbox.domain.project.repository.ProjectMemberRepository;
import com.blackbox.domain.score.dto.ScoreDto;
import com.blackbox.domain.score.service.ScoreEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiAnalyzerService {

    private final ActivityLogRepository activityLogRepository;
    private final ProjectMemberRepository memberRepository;
    private final ScoreEngine scoreEngine;
    private final OpenAiApiService openAiApiService;

    public String analyze(Long projectId) {
        // 팀원 정보
        List<ProjectMember> members = memberRepository.findByProjectId(projectId);
        Map<Long, String> nameMap = members.stream()
                .collect(Collectors.toMap(
                        m -> m.getUser().getId(),
                        m -> m.isConsentAi() ? m.getUser().getName() : "익명사용자_" + m.getUser().getId()
                ));

        // 최근 50개 활동 로그
        List<ActivityLog> logs = activityLogRepository.findByProjectIdOrderByCreatedAtDesc(
                projectId, PageRequest.of(0, 50));

        // 팀원별 이벤트 집계
        Map<String, Map<String, Long>> memberEvents = new LinkedHashMap<>();
        for (ActivityLog log : logs) {
            String name = nameMap.getOrDefault(log.getUser().getId(), "Unknown");
            memberEvents
                    .computeIfAbsent(name, k -> new LinkedHashMap<>())
                    .merge(log.getEventType().name(), 1L, Long::sum);
        }

        // 점수 리포트
        ScoreDto.ProjectScoreReport report;
        try {
            report = scoreEngine.getReport(projectId);
        } catch (Exception e) {
            report = null;
        }

        // 프롬프트 생성
        String prompt = buildPrompt(nameMap, memberEvents, report, logs.size());
        return openAiApiService.completeWithTokens(prompt, 800);
    }

    private String buildPrompt(
            Map<Long, String> nameMap,
            Map<String, Map<String, Long>> memberEvents,
            ScoreDto.ProjectScoreReport report,
            int logCount) {

        StringBuilder sb = new StringBuilder();
        sb.append("당신은 대학 팀 프로젝트 기여도를 분석하는 AI입니다. 아래 데이터를 보고 팀 현황을 한국어로 분석해주세요.\n\n");

        sb.append("## 팀원별 최근 활동 집계 (최근 ").append(logCount).append("개 이벤트)\n");
        memberEvents.forEach((name, events) -> {
            sb.append("- ").append(name).append(": ");
            events.forEach((type, count) ->
                    sb.append(type).append("×").append(count).append(" "));
            sb.append("\n");
        });

        if (report != null && !report.getMembers().isEmpty()) {
            sb.append("\n## 현재 기여도 점수\n");
            report.getMembers().forEach(m -> {
                String displayName = nameMap.getOrDefault(m.getUserId(), "Unknown");
                sb.append("- ").append(displayName)
                        .append(": ").append(String.format("%.1f", m.getNormalizedScore()))
                        .append("점 (").append(m.getGrade()).append("등급)\n");
            });
        }

        sb.append("\n## 분석 요청\n");
        sb.append("다음 3가지를 각각 2-3문장으로 분석해주세요:\n");
        sb.append("1. **팀 전체 건강도**: 협업이 잘 이뤄지고 있는지, 주의할 점은?\n");
        sb.append("2. **기여 불균형 여부**: 특정 팀원이 과도하게 많거나 적은 기여를 하고 있나요?\n");
        sb.append("3. **개선 제안**: 팀 성과를 높이기 위해 구체적으로 무엇을 하면 좋을까요?\n");
        sb.append("\n형식: 각 항목 제목(굵게) 뒤에 분석 내용. 총 200자 이내로 간결하게.");

        return sb.toString();
    }
}
