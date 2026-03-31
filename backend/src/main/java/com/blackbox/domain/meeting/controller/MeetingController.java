package com.blackbox.domain.meeting.controller;

import com.blackbox.domain.meeting.dto.MeetingDto;
import com.blackbox.domain.meeting.service.MeetingService;
import com.blackbox.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<MeetingDto.Response> createMeeting(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user,
            @RequestBody MeetingDto.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(meetingService.createMeeting(projectId, user.getId(), req));
    }

    @GetMapping
    public ResponseEntity<List<MeetingDto.SummaryResponse>> getMeetings(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(meetingService.getMeetings(projectId));
    }

    @GetMapping("/{meetingId}")
    public ResponseEntity<MeetingDto.Response> getMeeting(
            @PathVariable Long projectId,
            @PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingService.getMeeting(projectId, meetingId));
    }

    @PutMapping("/{meetingId}")
    public ResponseEntity<MeetingDto.Response> updateMeeting(
            @PathVariable Long projectId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal User user,
            @RequestBody MeetingDto.UpdateRequest req) {
        return ResponseEntity.ok(meetingService.updateMeeting(projectId, meetingId, req));
    }

    @DeleteMapping("/{meetingId}")
    public ResponseEntity<Void> deleteMeeting(
            @PathVariable Long projectId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal User user) {
        meetingService.deleteMeeting(projectId, meetingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{meetingId}/checkin")
    public ResponseEntity<MeetingDto.Response> checkIn(
            @PathVariable Long projectId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal User user,
            @RequestBody MeetingDto.CheckinRequest req) {
        return ResponseEntity.ok(meetingService.checkIn(projectId, meetingId, user.getId(), req));
    }

    @PostMapping("/{meetingId}/action-items")
    public ResponseEntity<Void> createActionItem(
            @PathVariable Long projectId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal User user,
            @RequestBody MeetingDto.ActionItemRequest req) {
        meetingService.createActionItem(projectId, meetingId, user.getId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
