package com.blackbox.domain.score.controller;

import com.blackbox.domain.score.dto.ProfessorDto;
import com.blackbox.domain.score.service.ProfessorService;
import com.blackbox.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/professor")
@RequiredArgsConstructor
public class ProfessorController {

    private final ProfessorService professorService;

    @GetMapping("/projects-overview")
    public ResponseEntity<List<ProfessorDto.ProjectOverviewResponse>> getProjectsOverview(
            @AuthenticationPrincipal User user) {
        
        // 권한 체크 등은 Security 수준이나 Service에서 담당.
        return ResponseEntity.ok(professorService.getProjectsOverview(user.getId()));
    }
}
