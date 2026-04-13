package com.blackbox.domain.google.dto;

import java.time.Instant;

public class GoogleDto {

    /** 연동 정보 응답 */
    public record InstallationResponse(
            Long projectId,
            boolean connected,
            String driveFolderId,
            String sheetId,
            String formId,
            Instant connectedAt
    ) {}

    /** 리소스 ID 설정 요청 */
    public record ResourceRequest(
            String driveFolderId,
            String sheetId,
            String formId
    ) {}

    /** 유저 매핑 요청 */
    public record MappingRequest(
            Long userId,
            String googleEmail
    ) {}

    /** 유저 매핑 응답 */
    public record MappingResponse(
            Long id,
            Long userId,
            String userName,
            String googleEmail
    ) {}

    /** 폴링 결과 */
    public record PollResult(
            int driveFiles,
            int sheetsEdits,
            int formResponses,
            int total
    ) {}
}
