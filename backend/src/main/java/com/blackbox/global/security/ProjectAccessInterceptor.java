package com.blackbox.global.security;

import com.blackbox.domain.project.repository.ProjectMemberRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.global.exception.BusinessException;
import com.blackbox.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class ProjectAccessInterceptor implements HandlerInterceptor {

    private final ProjectMemberRepository projectMemberRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) return true;

        // 교수 · TA는 모든 프로젝트 접근 허용
        if (user.getRole() == User.Role.PROFESSOR || user.getRole() == User.Role.TA) return true;

        // context-path 제외한 서블릿 경로에서 projectId 추출: /projects/{projectId}/...
        String path = request.getServletPath();
        String[] segments = path.split("/");
        // segments[0]="" segments[1]="projects" segments[2]="{projectId}"
        if (segments.length < 3) return true;
        Long projectId;
        try {
            projectId = Long.parseLong(segments[2]);
        } catch (NumberFormatException e) {
            return true; // projectId가 숫자가 아니면 패스 (다른 라우트)
        }

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return true;
    }
}
