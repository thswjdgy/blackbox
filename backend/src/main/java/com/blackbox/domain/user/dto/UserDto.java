package com.blackbox.domain.user.dto;

import java.time.Instant;

public class UserDto {
    public record ProfileResponse(
            Long id,
            String email,
            String name,
            String role,
            boolean dataCollectionConsent,
            Instant consentAt
    ) {}

    public record ConsentRequest(
            boolean dataCollectionConsent
    ) {}
}
