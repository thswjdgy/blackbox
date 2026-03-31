package com.blackbox.domain.auth.service;

import com.blackbox.domain.auth.dto.AuthDto;
import com.blackbox.domain.auth.entity.RefreshToken;
import com.blackbox.domain.auth.repository.RefreshTokenRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.domain.user.repository.UserRepository;
import com.blackbox.global.exception.BusinessException;
import com.blackbox.global.exception.ErrorCode;
import com.blackbox.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public AuthDto.TokenResponse signup(AuthDto.SignupRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User.Role role = User.Role.STUDENT;
        if (req.role() != null) {
            try { role = User.Role.valueOf(req.role().toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .name(req.name())
                .studentId(req.studentId())
                .role(role)
                .build();
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthDto.TokenResponse login(AuthDto.LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthDto.TokenResponse refresh(AuthDto.RefreshRequest req) {
        RefreshToken stored = refreshTokenRepository.findByToken(req.refreshToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        // 토큰 로테이션: 기존 삭제 후 새 발급
        User user = stored.getUser();
        refreshTokenRepository.delete(stored);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr)
                .ifPresent(refreshTokenRepository::delete);
    }

    private AuthDto.TokenResponse issueTokens(User user) {
        String accessToken = jwtProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String rawRefresh = jwtProvider.generateRefreshToken(user.getId());

        // 기존 리프레시 토큰 삭제 후 새로 발급 (중복 방지)
        refreshTokenRepository.deleteAllByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(rawRefresh)
                .expiresAt(Instant.now().plusMillis(jwtProvider.getRefreshExpirationMs()))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthDto.TokenResponse(
                accessToken,
                rawRefresh,
                new AuthDto.UserInfo(user.getId(), user.getEmail(), user.getName(), user.getRole().name())
        );
    }
}
