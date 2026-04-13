package com.blackbox.domain.google.service;

import com.blackbox.domain.google.dto.GoogleDto;
import com.blackbox.domain.google.entity.GoogleInstallation;
import com.blackbox.domain.google.entity.GoogleUserMapping;
import com.blackbox.domain.google.repository.GoogleInstallationRepository;
import com.blackbox.domain.google.repository.GoogleUserMappingRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private static final String GOOGLE_TOKEN_URL  = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_AUTH_URL   = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String SCOPES =
            "https://www.googleapis.com/auth/drive.readonly " +
            "https://www.googleapis.com/auth/spreadsheets.readonly " +
            "https://www.googleapis.com/auth/forms.responses.readonly " +
            "email profile";

    private final GoogleInstallationRepository installationRepository;
    private final GoogleUserMappingRepository  mappingRepository;
    private final ProjectRepository            projectRepository;
    private final UserRepository               userRepository;
    private final RestTemplate                 restTemplate;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    /** Google OAuth 인증 URL 생성 (state = projectId) */
    public String buildAuthUrl(Long projectId) {
        return UriComponentsBuilder.fromHttpUrl(GOOGLE_AUTH_URL)
                .queryParam("client_id",     clientId)
                .queryParam("redirect_uri",  redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope",         SCOPES)
                .queryParam("access_type",   "offline")
                .queryParam("prompt",        "consent")
                .queryParam("state",         projectId.toString())
                .build().toUriString();
    }

    /** OAuth 콜백 처리 — code를 토큰으로 교환 후 저장 */
    @Transactional
    public void handleCallback(String code, Long projectId) {
        Map<String, Object> tokenResponse = exchangeCode(code);
        if (tokenResponse == null) throw new IllegalStateException("Google 토큰 교환 실패");

        String accessToken  = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        Integer expiresIn   = (Integer) tokenResponse.get("expires_in");

        if (refreshToken == null) {
            throw new IllegalStateException("refresh_token이 없습니다. Google 계정에서 앱 권한을 제거 후 다시 연동하세요.");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        GoogleInstallation inst = installationRepository.findByProjectId(projectId)
                .orElse(GoogleInstallation.builder().project(project).build());

        inst.setAccessToken(accessToken);
        inst.setRefreshToken(refreshToken);
        inst.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
        installationRepository.save(inst);

        log.info("Google 연동 완료 [project={}]", projectId);
    }

    /** 연동 해제 */
    @Transactional
    public void unlink(Long projectId) {
        installationRepository.findByProjectId(projectId)
                .ifPresent(installationRepository::delete);
    }

    /** 연동 정보 조회 */
    @Transactional(readOnly = true)
    public GoogleDto.InstallationResponse getInstallation(Long projectId) {
        return installationRepository.findByProjectId(projectId)
                .map(i -> new GoogleDto.InstallationResponse(
                        projectId, true,
                        i.getDriveFolderId(), i.getSheetId(), i.getFormId(),
                        i.getConnectedAt()))
                .orElse(new GoogleDto.InstallationResponse(projectId, false, null, null, null, null));
    }

    /** 리소스 ID 업데이트 (Drive 폴더, Sheet, Form) */
    @Transactional
    public GoogleDto.InstallationResponse updateResources(Long projectId, GoogleDto.ResourceRequest req) {
        GoogleInstallation inst = installationRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTEGRATION_NOT_FOUND));
        if (req.driveFolderId() != null) inst.setDriveFolderId(req.driveFolderId());
        if (req.sheetId()       != null) inst.setSheetId(req.sheetId());
        if (req.formId()        != null) inst.setFormId(req.formId());
        installationRepository.save(inst);
        return getInstallation(projectId);
    }

    /** 유저 매핑 목록 */
    @Transactional(readOnly = true)
    public List<GoogleDto.MappingResponse> getMappings(Long projectId) {
        return mappingRepository.findByProjectId(projectId).stream()
                .map(m -> new GoogleDto.MappingResponse(
                        m.getId(), m.getUser().getId(),
                        m.getUser().getName(), m.getGoogleEmail()))
                .collect(Collectors.toList());
    }

    /** 유저 매핑 추가 */
    @Transactional
    public GoogleDto.MappingResponse addMapping(Long projectId, GoogleDto.MappingRequest req) {
        if (mappingRepository.existsByProjectIdAndUserId(projectId, req.userId()))
            throw new BusinessException(ErrorCode.ALREADY_MAPPED);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        GoogleUserMapping mapping = mappingRepository.save(
                GoogleUserMapping.builder()
                        .project(project).user(user)
                        .googleEmail(req.googleEmail())
                        .build());

        return new GoogleDto.MappingResponse(
                mapping.getId(), user.getId(), user.getName(), mapping.getGoogleEmail());
    }

    /** 유저 매핑 삭제 */
    @Transactional
    public void deleteMapping(Long projectId, Long mappingId) {
        mappingRepository.deleteById(mappingId);
    }

    /** access_token 갱신 (만료 시 자동 호출) */
    public String refreshAccessToken(GoogleInstallation inst) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type",    "refresh_token");
            params.add("refresh_token", inst.getRefreshToken());
            params.add("client_id",     clientId);
            params.add("client_secret", clientSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                    GOOGLE_TOKEN_URL, HttpMethod.POST,
                    new HttpEntity<>(params, headers),
                    new ParameterizedTypeReference<>() {});

            Map<String, Object> body = res.getBody();
            if (body == null) return null;

            String newToken = (String) body.get("access_token");
            Integer expiresIn = (Integer) body.get("expires_in");
            inst.setAccessToken(newToken);
            inst.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
            installationRepository.save(inst);
            return newToken;
        } catch (Exception e) {
            log.warn("Google access_token 갱신 실패: {}", e.getMessage());
            return null;
        }
    }

    /** 유효한 access_token 반환 (만료 시 자동 갱신) */
    public String getValidAccessToken(GoogleInstallation inst) {
        if (inst.getTokenExpiresAt() == null ||
                Instant.now().isAfter(inst.getTokenExpiresAt().minusSeconds(60))) {
            return refreshAccessToken(inst);
        }
        return inst.getAccessToken();
    }

    /* ── code → token 교환 ── */
    private Map<String, Object> exchangeCode(String code) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code",          code);
            params.add("client_id",     clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri",  redirectUri);
            params.add("grant_type",    "authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                    GOOGLE_TOKEN_URL, HttpMethod.POST,
                    new HttpEntity<>(params, headers),
                    new ParameterizedTypeReference<>() {});
            return res.getBody();
        } catch (Exception e) {
            log.warn("Google code 교환 실패: {}", e.getMessage());
            return null;
        }
    }
}
