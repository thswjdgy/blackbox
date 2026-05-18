package com.blackbox.domain.ai.service;

import com.blackbox.domain.activity.entity.ActivityLog;
import com.blackbox.domain.activity.repository.ActivityLogRepository;
import com.blackbox.domain.ai.entity.AiAnalysisResult;
import com.blackbox.domain.ai.repository.AiAnalysisResultRepository;
import com.blackbox.domain.project.entity.ProjectMember;
import com.blackbox.domain.project.repository.ProjectMemberRepository;
import com.blackbox.domain.score.dto.ScoreDto;
import com.blackbox.domain.score.service.ScoreEngine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalyzerService {

    private final ActivityLogRepository activityLogRepository;
    private final ProjectMemberRepository memberRepository;
    private final ScoreEngine scoreEngine;
    private final OpenAiApiService openAiApiService;
    private final AiAnalysisResultRepository analysisResultRepository;
    private final ObjectMapper objectMapper;

    /** 커밋 메시지 품질 분석 — 최근 N건 GITHUB_PUSH 이벤트 대상 */
    public List<Map<String, Object>> analyzeCommitQuality(Long projectId, int limit) {
        limit = Math.min(limit, 20);
        List<ActivityLog> pushes = activityLogRepository
                .findByProjectIdAndEventTypeOrderByCreatedAtDesc(
                        projectId, ActivityLog.EventType.GITHUB_PUSH,
                        org.springframework.data.domain.PageRequest.of(0, limit));

        if (pushes.isEmpty()) {
            return List.of();
        }

        // 커밋 목록 추출
        List<Map<String, Object>> commits = pushes.stream().map(log -> {
            Map<String, Object> payload = log.getPayload() != null ? log.getPayload() : Map.of();
            Map<String, Object> item = new HashMap<>();
            item.put("sha",     String.valueOf(payload.getOrDefault("sha", "")).substring(0, Math.min(7, String.valueOf(payload.getOrDefault("sha", "")).length())));
            item.put("message", String.valueOf(payload.getOrDefault("message", "(메시지 없음)")));
            item.put("author",  log.getUser().getName());
            return item;
        }).collect(java.util.stream.Collectors.toList());

        // 프롬프트 구성
        StringBuilder sb = new StringBuilder();
        sb.append("아래 Git 커밋 메시지들의 품질을 평가해주세요. 각 커밋에 대해 1-5점 점수와 한 줄 피드백을 한국어로 작성하세요.\n\n");
        sb.append("평가 기준: 1=매우 불량(\"fix\", \"update\" 같은 모호한 메시지), 3=보통, 5=우수(Conventional Commit 형식, 명확한 변경 범위)\n\n");
        sb.append("커밋 목록:\n");
        for (int i = 0; i < commits.size(); i++) {
            Map<String, Object> c = commits.get(i);
            sb.append(i + 1).append(". [").append(c.get("sha")).append("] ")
              .append(c.get("author")).append(": ").append(c.get("message")).append("\n");
        }
        sb.append("\n응답 형식 (반드시 지킬 것):\n");
        sb.append("1. 점수: N/5 | 평가: (한 줄 피드백)\n");
        sb.append("2. 점수: N/5 | 평가: (한 줄 피드백)\n");
        sb.append("...\n");

        String aiResponse = openAiApiService.completeWithTokens(sb.toString(), 600);

        // 응답 파싱: "N. 점수: X/5 | 평가: ..." 형식
        List<Map<String, Object>> results = new ArrayList<>();
        String[] lines = aiResponse.split("\n");
        int idx = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || idx >= commits.size()) continue;
            // "1. 점수: 4/5 | 평가: ..." 형식 파싱
            if (line.matches("^\\d+\\..*점수.*")) {
                int score = 3; // 기본값
                String feedback = line;
                try {
                    java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("점수[:\\s]+(\\d)/5").matcher(line);
                    if (m.find()) score = Integer.parseInt(m.group(1));
                    int pipeIdx = line.indexOf("평가:");
                    if (pipeIdx >= 0) feedback = line.substring(pipeIdx + 3).trim();
                } catch (Exception ignored) {}
                Map<String, Object> commit = commits.get(idx);
                results.add(Map.of(
                        "sha",      commit.get("sha"),
                        "message",  commit.get("message"),
                        "author",   commit.get("author"),
                        "score",    score,
                        "feedback", feedback
                ));
                idx++;
            }
        }
        // 파싱 실패 시 commits 그대로 반환 (score=0)
        if (results.isEmpty()) {
            for (Map<String, Object> c : commits) {
                results.add(new HashMap<>(Map.of(
                        "sha", c.get("sha"), "message", c.get("message"),
                        "author", c.get("author"), "score", 0, "feedback", aiResponse
                )));
            }
        }

        // 결과 DB 저장
        try {
            String json = objectMapper.writeValueAsString(results);
            analysisResultRepository.save(AiAnalysisResult.builder()
                    .projectId(projectId)
                    .analysisType(AiAnalysisResult.AnalysisType.COMMIT)
                    .result(json)
                    .build());
        } catch (Exception e) {
            log.warn("커밋 분석 결과 저장 실패: {}", e.getMessage());
        }
        return results;
    }

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

        // 프롬프트 생성 및 API 호출
        String prompt = buildPrompt(nameMap, memberEvents, report, logs.size());
        String aiResult = openAiApiService.completeWithTokens(prompt, 800);

        // 결과 DB 저장
        analysisResultRepository.save(AiAnalysisResult.builder()
                .projectId(projectId)
                .analysisType(AiAnalysisResult.AnalysisType.TEAM)
                .result(aiResult)
                .build());

        return aiResult;
    }

    /** 마지막 팀 분석 결과 조회 (캐시) */
    public Optional<AiAnalysisResult> getLatestTeamAnalysis(Long projectId) {
        return analysisResultRepository.findTopByProjectIdAndAnalysisTypeOrderByCreatedAtDesc(
                projectId, AiAnalysisResult.AnalysisType.TEAM);
    }

    /** 마지막 커밋 품질 분석 엔티티 조회 */
    public Optional<AiAnalysisResult> getLatestCommitResult(Long projectId) {
        return analysisResultRepository.findTopByProjectIdAndAnalysisTypeOrderByCreatedAtDesc(
                projectId, AiAnalysisResult.AnalysisType.COMMIT);
    }

    public List<Map<String, Object>> parseCommitResult(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
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
