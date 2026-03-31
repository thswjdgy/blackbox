package com.blackbox.domain.meeting.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MeetingAttendeeId implements Serializable {
    private Long meetingId;
    private Long userId;
}
