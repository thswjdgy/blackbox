package com.blackbox.domain.meeting.repository;

import com.blackbox.domain.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByProjectIdOrderByMeetingAtDesc(Long projectId);
}
