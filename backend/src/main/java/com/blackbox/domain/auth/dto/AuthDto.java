package com.blackbox.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {

    public record SignupRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            @NotBlank @Size(max = 50) String name,
            String studentId,
            String role  // STUDENT | PROFESSOR | TA (기본값: STUDENT)
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            UserInfo user
    ) {}

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {}

    public record UserInfo(
            Long id,
            String email,
            String name,
            String role
    ) {}
}
