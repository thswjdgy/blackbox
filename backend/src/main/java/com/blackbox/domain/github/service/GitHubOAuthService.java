package com.blackbox.domain.github.service;

import com.blackbox.domain.github.entity.GitHubUserMapping;
import com.blackbox.domain.github.repository.GitHubUserMappingRepository;
import com.blackbox.domain.project.entity.Project;
import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.domain.user.repository.UserRepository;
import com.blackbox.global.exception.BusinessException;
import com.blackbox.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubOAuthService {

    private static final String GITHUB_AUTH_URL  = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_USER_URL  = "https://api.github.com/user";

    private final GitHubUserMappingRepository mappingRepository;
    private final ProjectRepository           projectRepository;
    private final UserRepository              userRepository;
    private final RestTemplate                restTemplate;

    @Value("${github.client-id:}")
    private String clientId;

    @Value("${github.client-secret:}")
    private String clientSecret;

    @Value("${github.redirect-uri:http://localhost/api/github/oauth/callback}")
    private String redirectUri;

    /** OAuth 인증 URL 생성 — state = "{projectId}:{userId}" */
    public String buildAuthUrl(Long projectId, Long userId) {
        return UriComponentsBuilder.fromUriString(GITHUB_AUTH_URL)
                .queryParam("client_id",    clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope",        "read:user,user:email")
                .queryParam("state",        projectId + ":" + userId)
                .build().toUriString();
    }

    /** OAuth 콜백 처리 — code → access_token → GitHub 유저 → 매핑 저장 */
    @Transactional
    public void handleCallback(String code, String state) {
        String[] parts = state.split(":");
        if (parts.length != 2) throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 state 파라미터");
        Long projectId = Long.parseLong(parts[0]);
        Long userId    = Long.parseLong(parts[1]);

        // 1. code → access_token
        String accessToken = exchangeCode(code);

        // 2. access_token → GitHub 유저 정보
        Map<String, Object> ghUser = fetchGitHubUser(accessToken);
        String ghLogin = String.valueOf(ghUser.get("login"));

        // 3. 플랫폼 유저 조회
        User user    = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Project proj = projectRepository.findById(projectId).orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        // 4. 이미 매핑 존재하면 업데이트, 없으면 생성
        GitHubUserMapping mapping = mappingRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseGet(() -> GitHubUserMapping.builder().project(proj).user(user).build());
        mapping.setGithubLogin(ghLogin);
        mappingRepository.save(mapping);
        log.info("GitHub OAuth 매핑 완료: userId={} → @{}", userId, ghLogin);
    }

    private String exchangeCode(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id",     clientId);
        body.add("client_secret", clientSecret);
        body.add("code",          code);
        body.add("redirect_uri",  redirectUri);

        ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                GITHUB_TOKEN_URL, HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<>() {});

        if (res.getBody() == null || !res.getBody().containsKey("access_token")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "GitHub 토큰 발급 실패");
        }
        return String.valueOf(res.getBody().get("access_token"));
    }

    private Map<String, Object> fetchGitHubUser(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/vnd.github+json");

        ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                GITHUB_USER_URL, HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {});

        if (res.getBody() == null) throw new BusinessException(ErrorCode.INVALID_INPUT, "GitHub 유저 정보 조회 실패");
        return res.getBody();
    }
}
