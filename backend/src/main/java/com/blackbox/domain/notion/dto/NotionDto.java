package com.blackbox.domain.notion.dto;

import java.time.Instant;

public class NotionDto {

    public record LinkRequest(
            String integrationToken,  // Notion Internal Integration Token (secret_xxx)
            String databaseId,        // optional: 특정 DB ID만 폴링
            String workspaceName      // optional: 표시용 이름
    ) {}

    public record InstallationResponse(
            Long    id,
            Long    projectId,
            boolean hasToken,
            String  databaseId,
            String  workspaceName,
            Instant lastPolledAt,
            Instant connectedAt
    ) {}

    public record MappingRequest(
            Long   userId,
            String notionUserId,
            String notionUserName
    ) {}

    public record MappingResponse(
            Long   id,
            Long   userId,
            String userName,
            String notionUserId,
            String notionUserName
    ) {}

    public record PollResult(
            int created,
            int edited,
            int total
    ) {}
}
