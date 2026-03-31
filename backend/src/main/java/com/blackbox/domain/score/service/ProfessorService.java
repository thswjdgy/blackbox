package com.blackbox.domain.score.service;

import com.blackbox.domain.project.entity.Project;
import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.score.dto.ProfessorDto;
import com.blackbox.domain.score.entity.ContributionScore;
import com.blackbox.domain.score.repository.AlertRepository;
import com.blackbox.domain.score.repository.ContributionScoreRepository;
import com.blackbox.domain.task.entity.Task;
import com.blackbox.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessorService {

    private final ProjectRepository projectRepository;
    private final ContributionScoreRepository contributionScoreRepository;
    private final AlertRepository alertRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<ProfessorDto.ProjectOverviewResponse> getProjectsOverview(Long professorUserId) {
        // 교수가 속해있거나, 교수가 권한을 가진 전체 프로젝트를 조회. (현재는 Mocking으로 교수를 모든 활성 프로젝트의 감시자로 가정)
        // 만약 특정 교수만 보고 싶다면 projectRepository.findByMembersUserId(professorUserId) 사용.
        List<Project> activeProjects = projectRepository.findAll().stream()
                .filter(Project::isActive)
                .collect(Collectors.toList());

        return activeProjects.stream().map(project -> {
            Long projectId = project.getId();
            
            // 1. 점수 조회 (평균 계산)
            List<ContributionScore> scores = contributionScoreRepository.findByProjectIdOrderByNormalizedScoreDesc(projectId);
            double avgScore = scores.isEmpty() ? 0.0 : 
                    scores.stream().mapToDouble(ContributionScore::getTotalScore).average().orElse(0.0);

            // 2. 진행 중인 이슈(Alert) 건수
            int alertCount = alertRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                    .filter(a -> !a.isResolved())
                    .toList().size();

            // 3. 태스크 달성률
            List<Task> tasks = taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
            int totalTasks = tasks.size();
            int completedTasks = (int) tasks.stream().filter(t -> t.getStatus() == Task.Status.DONE).count();

            return ProfessorDto.ProjectOverviewResponse.builder()
                    .projectId(projectId)
                    .projectName(project.getName())
                    .memberCount(project.getMembers().size())
                    .averageScore(Math.round(avgScore * 100.0) / 100.0)
                    .alertCount(alertCount)
                    .totalTasks(totalTasks)
                    .completedTasks(completedTasks)
                    .build();
        }).collect(Collectors.toList());
    }
}
