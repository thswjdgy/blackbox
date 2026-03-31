package com.blackbox.domain.task.dto;

import com.blackbox.domain.task.entity.Task.Priority;
import com.blackbox.domain.task.entity.Task.Status;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

public class TaskDto {

    @Getter
    public static class CreateRequest {
        private String title;
        private String description;
        private Priority priority;
        private String tag;
        private Instant dueDate;
        private List<Long> assigneeIds;
    }

    @Getter
    public static class UpdateRequest {
        private String title;
        private String description;
        private Priority priority;
        private String tag;
        private Instant dueDate;
        private List<Long> assigneeIds;
    }

    @Getter
    public static class StatusUpdateRequest {
        private Status status;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long projectId;
        private Long meetingId;
        private String title;
        private String description;
        private Status status;
        private Priority priority;
        private String tag;
        private Instant dueDate;
        private Instant completedAt;
        private Long createdById;
        private List<Long> assigneeIds;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
