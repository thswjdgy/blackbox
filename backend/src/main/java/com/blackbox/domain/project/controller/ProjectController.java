package com.blackbox.domain.project.controller;

import com.blackbox.domain.project.dto.ProjectDto;
import com.blackbox.domain.project.service.ProjectService;
import com.blackbox.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /** GET /api/projects — 내 프로젝트 목록 */
    @GetMapping
    public ResponseEntity<List<ProjectDto.ProjectResponse>> getMyProjects(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.getMyProjects(user));
    }

    /** GET /api/projects/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto.ProjectDetailResponse> getProject(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.getProject(id, user));
    }

    /** POST /api/projects */
    @PostMapping
    public ResponseEntity<ProjectDto.ProjectDetailResponse> createProject(
            @Valid @RequestBody ProjectDto.CreateRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(req, user));
    }

    /** PATCH /api/projects/{id} */
    @PatchMapping("/{id}")
    public ResponseEntity<ProjectDto.ProjectDetailResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectDto.UpdateRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.updateProject(id, req, user));
    }

    /** POST /api/projects/join — 초대 코드 참여 */
    @PostMapping("/join")
    public ResponseEntity<ProjectDto.ProjectDetailResponse> joinByInviteCode(
            @Valid @RequestBody ProjectDto.JoinRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.joinByInviteCode(req, user));
    }

    /** GET /api/projects/{id}/members — 멤버 목록 */
    @GetMapping("/{id}/members")
    public ResponseEntity<List<ProjectDto.MemberResponse>> getMembers(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.getMembers(id, user));
    }

    /** PATCH /api/projects/{id}/members/{userId}/role — 역할 변경 */
    @PatchMapping("/{id}/members/{userId}/role")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody ProjectDto.UpdateRoleRequest req,
            @AuthenticationPrincipal User user) {
        projectService.updateMemberRole(id, userId, req, user);
        return ResponseEntity.noContent().build();
    }

    /** DELETE /api/projects/{id}/members/me — 탈퇴 */
    @DeleteMapping("/{id}/members/me")
    public ResponseEntity<Void> leaveProject(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        projectService.leaveProject(id, user);
        return ResponseEntity.noContent().build();
    }

    /** DELETE /api/projects/{id}/members/{userId} — 강퇴 (LEADER) */
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal User user) {
        projectService.removeMember(id, userId, user);
        return ResponseEntity.noContent().build();
    }
}
