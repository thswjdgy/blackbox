package com.blackbox.domain.score.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

public class WeightDto {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {
        @JsonProperty("wTask")    private double wTask;
        @JsonProperty("wMeeting") private double wMeeting;
        @JsonProperty("wFile")    private double wFile;
        @JsonProperty("wExtra")   private double wExtra;
    }

    @Getter
    @Builder
    public static class Response {
        @JsonProperty("projectId")  private Long projectId;
        @JsonProperty("wTask")      private double wTask;
        @JsonProperty("wMeeting")   private double wMeeting;
        @JsonProperty("wFile")      private double wFile;
        @JsonProperty("wExtra")     private double wExtra;
        @JsonProperty("updatedAt")  private Instant updatedAt;
    }

    @Getter
    @Builder
    public static class HistoryResponse {
        private Long id;
        private Long projectId;
        private Long changedById;
        private String changedByName;
        private double wTask;
        private double wMeeting;
        private double wFile;
        private double wExtra;
        private Instant changedAt;
    }
}
