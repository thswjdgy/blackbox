package com.blackbox.domain.github.service;

import com.blackbox.domain.activity.entity.ActivityLog;
import com.blackbox.domain.activity.repository.ActivityLogRepository;
import com.blackbox.domain.github.dto.GitHubDto;
import com.blackbox.domain.github.entity.GitHubInstallation;
import com.blackbox.domain.github.repository.GitHubInstallationRepository;
import com.blackbox.domain.github.repository.GitHubUserMappingRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubPollService {

    private final GitHubInstallationRepository installationRepository;
    private final GitHubUserMappingRepository  mappingRepository;
    private final ActivityLogRepository        activityLogRepository;
    private final UserRepository               userRepository;
    private final RestTemplate                 restTemplate;

    private static final String GH_API = "https://api.github.com";

    /** 30분마다 PAT가 등록된 모든 레포 폴링 */
    @Scheduled(fixedDelay = 1_800_000)
    public void pollAll() {
        List<GitHubInstallation> installations = installationRepository.findAllByGithubTokenIsNotNull();
        log.info("GitHub polling: {} repos", installations.size());
        installations.forEach(inst -> {
            try { poll(inst); } catch (Exception e) {
                log.warn("Poll failed for {}: {}", inst.getRepoFullName(), e.getMessage());
            }
        });
    }

    /** 수동 즉시 폴링 */
    @Transactional
    public GitHubDto.PollResult pollNow(Long projectId) {
        GitHubInstallation inst = installationRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalStateException("GitHub 연동 없음"));
        return poll(inst);
    }

    @Transactional
    protected GitHubDto.PollResult poll(GitHubInstallation inst) {
        Instant since = inst.getLastPolledAt() != null
                ? inst.getLastPolledAt()
                : Instant.now().minusSeconds(60 * 60 * 24 * 7); // 최초: 7일치

        String[] parts   = inst.getRepoFullName().split("/", 2);
        String   owner   = parts[0];
        String   repo    = parts[1];
        Long     projId  = inst.getProject().getId();

        int commits  = fetchAndLogCommits(inst, owner, repo, projId, since);
        int prs      = fetchAndLogPRs(inst, owner, repo, projId, since);

        inst.setLastPolledAt(Instant.now());
        installationRepository.save(inst);

        log.info("GitHub poll [{}]: {} commits, {} PRs", inst.getRepoFullName(), commits, prs);
        return new GitHubDto.PollResult(inst.getRepoFullName(), commits + prs, 0);
    }

    /* ── 커밋 수집 ── */
    private int fetchAndLogCommits(GitHubInstallation inst, String owner, String repo,
                                   Long projectId, Instant since) {
        String url = UriComponentsBuilder
                .fromUriString(GH_API + "/repos/{owner}/{repo}/commits")
                .queryParam("since", DateTimeFormatter.ISO_INSTANT.format(since))
                .queryParam("per_page", 100)
                .buildAndExpand(owner, repo).toUriString();

        List<GitHubDto.GhCommit> commits = getList(url, inst.getGithubToken(),
                new ParameterizedTypeReference<>() {});
        if (commits == null) return 0;

        int count = 0;
        for (GitHubDto.GhCommit c : commits) {
            if (c.commit() == null) continue;
            String email  = c.commit().author() != null ? c.commit().author().email() : null;
            String login  = c.author() != null ? c.author().login() : null;
            String message = c.commit().message();

            Optional<User> user = resolveUser(projectId, login, email);
            if (user.isEmpty()) continue;

            // 중복 방지: sha를 payload에 저장 후 체크
            if (activityLogRepository.existsByProjectIdAndSourceAndPayloadSha(
                    projectId, "GITHUB", c.sha())) continue;

            activityLogRepository.save(ActivityLog.builder()
                    .project(inst.getProject())
                    .user(user.get())
                    .eventType(ActivityLog.EventType.GITHUB_PUSH)
                    .source("GITHUB")
                    .payload(Map.of(
                            "sha",     c.sha(),
                            "message", message != null ? message.lines().findFirst().orElse("") : "",
                            "repo",    inst.getRepoFullName()
                    ))
                    .build());
            count++;
        }
        return count;
    }

    /* ── PR 수집 ── */
    private int fetchAndLogPRs(GitHubInstallation inst, String owner, String repo,
                               Long projectId, Instant since) {
        String url = UriComponentsBuilder
                .fromUriString(GH_API + "/repos/{owner}/{repo}/pulls")
                .queryParam("state", "all")
                .queryParam("sort", "updated")
                .queryParam("direction", "desc")
                .queryParam("per_page", 50)
                .buildAndExpand(owner, repo).toUriString();

        List<GitHubDto.GhPullRequest> prs = getList(url, inst.getGithubToken(),
                new ParameterizedTypeReference<>() {});
        if (prs == null) return 0;

        int count = 0;
        for (GitHubDto.GhPullRequest pr : prs) {
            String login = pr.user() != null ? pr.user().login() : null;
            Optional<User> user = resolveUser(projectId, login, null);
            if (user.isEmpty()) continue;

            ActivityLog.EventType type = pr.merged()
                    ? ActivityLog.EventType.GITHUB_PR_MERGED
                    : ActivityLog.EventType.GITHUB_PR_OPENED;

            String key = "pr-" + pr.number() + "-" + type.name();
            if (activityLogRepository.existsByProjectIdAndSourceAndPayloadSha(
                    projectId, "GITHUB", key)) continue;

            activityLogRepository.save(ActivityLog.builder()
                    .project(inst.getProject())
                    .user(user.get())
                    .eventType(type)
                    .source("GITHUB")
                    .payload(Map.of(
                            "sha",    key,
                            "pr",     pr.number(),
                            "title",  pr.title() != null ? pr.title() : "",
                            "repo",   inst.getRepoFullName()
                    ))
                    .build());
            count++;
        }
        return count;
    }

    /* ── 유저 매핑 ── */
    private Optional<User> resolveUser(Long projectId, String login, String email) {
        // 1. 수동 매핑 (GitHub login → user)
        if (login != null) {
            var mapping = mappingRepository.findByProjectIdAndGithubLogin(projectId, login);
            if (mapping.isPresent()) return Optional.of(mapping.get().getUser());
        }
        // 2. 이메일 자동 매핑
        if (email != null) {
            return userRepository.findByEmail(email);
        }
        return Optional.empty();
    }

    /* ── HTTP ── */
    private <T> List<T> getList(String url, String token,
                                ParameterizedTypeReference<List<T>> type) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Accept", "application/vnd.github+json");
            headers.set("X-GitHub-Api-Version", "2022-11-28");

            ResponseEntity<List<T>> res = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), type);
            return res.getBody();
        } catch (Exception e) {
            log.warn("GitHub API error [{}]: {}", url, e.getMessage());
            return null;
        }
    }
}
