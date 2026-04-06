package com.blackbox.domain.github.dto;

import java.time.Instant;

public class GitHubDto {

    public record LinkRequest(
            String repoFullName,   // "owner/repo"
            String githubToken,    // Personal Access Token
            String webhookSecret   // optional
    ) {}

    public record InstallationResponse(
            Long id,
            Long projectId,
            String repoFullName,
            boolean hasToken,
            Instant lastPolledAt,
            Instant connectedAt
    ) {}

    public record MappingRequest(
            Long userId,
            String githubLogin
    ) {}

    public record MappingResponse(
            Long id,
            Long userId,
            String userName,
            String githubLogin
    ) {}

    public record PollResult(
            String repo,
            int commitsProcessed,
            int usersMatched
    ) {}

    // GitHub API 응답 파싱용 내부 record
    public record GhCommit(
            String sha,
            GhCommitDetail commit,
            GhAuthor author  // nullable (GitHub user)
    ) {
        public record GhCommitDetail(
                String message,
                GhCommitAuthor author
        ) {}
        public record GhCommitAuthor(
                String name,
                String email,
                String date
        ) {}
    }

    public record GhAuthor(
            String login
    ) {}

    public record GhPullRequest(
            Long number,
            String title,
            String state,
            boolean merged,
            GhUser user
    ) {}

    public record GhUser(
            String login
    ) {}
}
