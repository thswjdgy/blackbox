package com.blackbox.domain.meeting.repository;

import com.blackbox.domain.meeting.entity.MeetingAttendee;
import com.blackbox.domain.meeting.entity.MeetingAttendeeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingAttendeeRepository extends JpaRepository<MeetingAttendee, MeetingAttendeeId> {
    boolean existsByIdMeetingIdAndIdUserId(Long meetingId, Long userId);
}
