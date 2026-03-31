package com.blackbox.domain.task.controller;

import com.blackbox.domain.task.dto.TaskDto;
import com.blackbox.domain.task.service.TaskService;
import com.blackbox.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskDto.Response> createTask(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user,
            @RequestBody TaskDto.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(projectId, user.getId(), req));
    }

    @GetMapping
    public ResponseEntity<List<TaskDto.Response>> getTasks(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasks(projectId));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskDto.Response> updateTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user,
            @RequestBody TaskDto.UpdateRequest req) {
        return ResponseEntity.ok(taskService.updateTask(taskId, user.getId(), req));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user,
            @RequestBody TaskDto.StatusUpdateRequest req) {
        taskService.updateStatus(taskId, user.getId(), req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {
        taskService.deleteTask(taskId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
