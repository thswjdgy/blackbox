package com.blackbox.domain.meeting.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

public class MeetingDto {

    @Getter
    public static class CreateRequest {
        private String title;
        private String purpose;
        private Instant meetingAt;
    }

    @Getter
    public static class UpdateRequest {
        private String title;
        private String purpose;
        private String notes;
        private String decisions;
        private Instant meetingAt;
    }

    @Getter
    public static class CheckinRequest {
        private String checkinCode;
    }

    @Getter
    public static class ActionItemRequest {
        private String title;
        private String description;
    }

    @Getter
    @Builder
    public static class AttendeeResponse {
        private Long userId;
        private String name;
        private Instant checkedInAt;
        private boolean present;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long projectId;
        private String title;
        private String purpose;
        private String notes;
        private String decisions;
        private String checkinCode;
        private String aiSummary;
        private String notionPageId;
        private Instant meetingAt;
        private Long createdById;
        private List<AttendeeResponse> attendees;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Getter
    @Builder
    public static class SummaryResponse {
        private Long id;
        private Long projectId;
        private String title;
        private Instant meetingAt;
        private int attendeeCount;
        private Instant createdAt;
    }
}
