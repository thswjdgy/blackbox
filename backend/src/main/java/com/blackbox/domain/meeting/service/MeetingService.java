package com.blackbox.domain.meeting.service;

import com.blackbox.domain.activity.entity.ActivityLog.EventType;
import com.blackbox.domain.activity.service.ActivityLogService;
import com.blackbox.domain.ai.service.OpenAiApiService;
import com.blackbox.domain.meeting.dto.MeetingDto;
import com.blackbox.domain.meeting.entity.Meeting;
import com.blackbox.domain.meeting.entity.MeetingAttendee;
import com.blackbox.domain.meeting.entity.MeetingAttendeeId;
import com.blackbox.domain.meeting.repository.MeetingAttendeeRepository;
import com.blackbox.domain.meeting.repository.MeetingRepository;
import com.blackbox.domain.project.entity.Project;
import com.blackbox.domain.project.entity.ProjectMember;
import com.blackbox.domain.project.repository.ProjectMemberRepository;
import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.task.entity.Task;
import com.blackbox.domain.task.repository.TaskRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.domain.user.repository.UserRepository;
import com.blackbox.global.exception.BusinessException;
import com.blackbox.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private static final String CHECKIN_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CHECKIN_CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MeetingRepository meetingRepository;
    private final MeetingAttendeeRepository meetingAttendeeRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ActivityLogService activityLogService;
    private final OpenAiApiService openAiApiService;

    @Transactional
    public MeetingDto.Response createMeeting(Long projectId, Long userId, MeetingDto.CreateRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Meeting meeting = Meeting.builder()
                .project(project)
                .title(req.getTitle())
                .purpose(req.getPurpose())
                .meetingAt(req.getMeetingAt())
                .checkinCode(generateCheckinCode())
                .createdBy(user)
                .build();

        meeting = meetingRepository.save(meeting);

        activityLogService.logMeetingEvent(
                project, user, meeting.getId(), EventType.MEETING_CREATED,
                Map.of("title", meeting.getTitle())
        );

        return toResponse(meeting);
    }

    @Transactional(readOnly = true)
    public List<MeetingDto.SummaryResponse> getMeetings(Long projectId) {
        return meetingRepository.findByProjectIdOrderByMeetingAtDesc(projectId).stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MeetingDto.Response getMeeting(Long projectId, Long meetingId) {
        Meeting meeting = findMeetingInProject(projectId, meetingId);
        return toResponse(meeting);
    }

    @Transactional
    public MeetingDto.Response updateMeeting(Long projectId, Long meetingId, MeetingDto.UpdateRequest req) {
        Meeting meeting = findMeetingInProject(projectId, meetingId);

        if (req.getTitle() != null) meeting.setTitle(req.getTitle());
        if (req.getPurpose() != null) meeting.setPurpose(req.getPurpose());
        if (req.getNotes() != null) meeting.setNotes(req.getNotes());
        if (req.getDecisions() != null) meeting.setDecisions(req.getDecisions());
        if (req.getMeetingAt() != null) meeting.setMeetingAt(req.getMeetingAt());

        return toResponse(meeting);
    }

    @Transactional
    public void deleteMeeting(Long projectId, Long meetingId) {
        Meeting meeting = findMeetingInProject(projectId, meetingId);
        meetingRepository.delete(meeting);
    }

    @Transactional
    public MeetingDto.Response checkIn(Long projectId, Long meetingId, Long userId, MeetingDto.CheckinRequest req) {
        Meeting meeting = findMeetingInProject(projectId, meetingId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!meeting.getCheckinCode().equals(req.getCheckinCode())) {
            throw new BusinessException(ErrorCode.CHECKIN_CODE_INVALID);
        }

        if (meetingAttendeeRepository.existsByIdMeetingIdAndIdUserId(meetingId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN);
        }

        MeetingAttendee attendee = MeetingAttendee.builder()
                .id(new MeetingAttendeeId(meetingId, userId))
                .meeting(meeting)
                .user(user)
                .checkedInAt(Instant.now())
                .build();
        meetingAttendeeRepository.save(attendee);
        meetingAttendeeRepository.flush();

        activityLogService.logMeetingEvent(
                meeting.getProject(), user, meetingId, EventType.MEETING_CHECKIN,
                Map.of("meetingTitle", meeting.getTitle())
        );

        // 컬렉션을 직접 수정하지 않고 DB에서 최신 상태 재조회
        Meeting refreshed = meetingRepository.findById(meetingId).orElseThrow();
        return toResponse(refreshed);
    }

    /** 관리자 수동 체크인 */
    @Transactional
    public MeetingDto.Response manualCheckin(Long projectId, Long meetingId, Long targetUserId) {
        Meeting meeting = findMeetingInProject(projectId, meetingId);
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!meetingAttendeeRepository.existsByIdMeetingIdAndIdUserId(meetingId, targetUserId)) {
            MeetingAttendee attendee = MeetingAttendee.builder()
                    .id(new MeetingAttendeeId(meetingId, targetUserId))
                    .meeting(meeting)
                    .user(user)
                    .checkedInAt(Instant.now())
                    .build();
            meetingAttendeeRepository.save(attendee);
            meetingAttendeeRepository.flush();
        }

        Meeting refreshed = meetingRepository.findById(meetingId).orElseThrow();
        return toResponse(refreshed);
    }

    /** 수동 체크인 취소 */
    @Transactional
    public MeetingDto.Response removeAttendee(Long projectId, Long meetingId, Long targetUserId) {
        findMeetingInProject(projectId, meetingId);
        meetingAttendeeRepository.deleteById(new MeetingAttendeeId(meetingId, targetUserId));
        meetingAttendeeRepository.flush();
        Meeting refreshed = meetingRepository.findById(meetingId).orElseThrow();
        return toResponse(refreshed);
    }

    /** AI 요약 생성 — Claude API 호출 후 meeting.aiSummary 저장 */
    @Transactional
    public MeetingDto.Response generateAiSummary(Long projectId, Long meetingId) {
        Meeting meeting = findMeetingInProject(projectId, meetingId);

        String prompt = buildSummaryPrompt(meeting);
        String summary = openAiApiService.complete(prompt);

        meeting.setAiSummary(summary);
        meetingRepository.save(meeting);

        return toResponse(meeting);
    }

    @Transactional
    public void createActionItem(Long projectId, Long meetingId, Long userId, MeetingDto.ActionItemRequest req) {
        Meeting meeting = findMeetingInProject(projectId, meetingId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Task task = Task.builder()
                .project(meeting.getProject())
                .meeting(meeting)
                .title(req.getTitle())
                .description(req.getDescription())
                .createdBy(user)
                .build();
        taskRepository.save(task);

        activityLogService.logTaskEvent(
                meeting.getProject(), user, task.getId(), EventType.TASK_CREATED,
                Map.of("title", task.getTitle(), "fromMeeting", meetingId.toString())
        );
    }

    private String buildSummaryPrompt(Meeting meeting) {
        StringBuilder sb = new StringBuilder();
        sb.append("아래 회의 내용을 3문장 이내로 핵심만 한국어로 요약해줘. ");
        sb.append("결정사항이 있으면 마지막에 '• 결정: ...' 형식으로 한 줄만 추가해.\n\n");
        sb.append("제목: ").append(meeting.getTitle()).append("\n");
        if (meeting.getPurpose() != null && !meeting.getPurpose().isBlank()) {
            sb.append("목적: ").append(meeting.getPurpose()).append("\n");
        }
        if (meeting.getNotes() != null && !meeting.getNotes().isBlank()) {
            sb.append("내용: ").append(meeting.getNotes()).append("\n");
        }
        if (meeting.getDecisions() != null && !meeting.getDecisions().isBlank()) {
            sb.append("결정: ").append(meeting.getDecisions()).append("\n");
        }
        return sb.toString();
    }

    private Meeting findMeetingInProject(Long projectId, Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));
        if (!meeting.getProject().getId().equals(projectId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return meeting;
    }

    private String generateCheckinCode() {
        StringBuilder sb = new StringBuilder(CHECKIN_CODE_LENGTH);
        for (int i = 0; i < CHECKIN_CODE_LENGTH; i++) {
            sb.append(CHECKIN_CODE_CHARS.charAt(RANDOM.nextInt(CHECKIN_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private MeetingDto.Response toResponse(Meeting meeting) {
        List<ProjectMember> members = projectMemberRepository.findByProjectId(meeting.getProject().getId());
        Map<Long, MeetingAttendee> attendeeMap = meeting.getAttendees().stream()
                .collect(Collectors.toMap(a -> a.getUser().getId(), a -> a));

        List<MeetingDto.AttendeeResponse> attendance = members.stream()
                .map(m -> {
                    MeetingAttendee a = attendeeMap.get(m.getUser().getId());
                    return MeetingDto.AttendeeResponse.builder()
                            .userId(m.getUser().getId())
                            .name(m.getUser().getName())
                            .checkedInAt(a != null ? a.getCheckedInAt() : null)
                            .present(a != null)
                            .build();
                })
                .sorted((a, b) -> {
                    if (a.isPresent() && !b.isPresent()) return -1;
                    if (!a.isPresent() && b.isPresent()) return 1;
                    return a.getName().compareTo(b.getName());
                })
                .collect(Collectors.toList());

        return MeetingDto.Response.builder()
                .id(meeting.getId())
                .projectId(meeting.getProject().getId())
                .title(meeting.getTitle())
                .purpose(meeting.getPurpose())
                .notes(meeting.getNotes())
                .decisions(meeting.getDecisions())
                .checkinCode(meeting.getCheckinCode())
                .aiSummary(meeting.getAiSummary())
                .notionPageId(meeting.getNotionPageId())
                .meetingAt(meeting.getMeetingAt())
                .createdById(meeting.getCreatedBy().getId())
                .attendees(attendance)
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .build();
    }

    private MeetingDto.SummaryResponse toSummaryResponse(Meeting meeting) {
        return MeetingDto.SummaryResponse.builder()
                .id(meeting.getId())
                .projectId(meeting.getProject().getId())
                .title(meeting.getTitle())
                .meetingAt(meeting.getMeetingAt())
                .attendeeCount(meeting.getAttendees().size())
                .createdAt(meeting.getCreatedAt())
                .build();
    }
}
