package com.blackbox.domain.task.service;

import com.blackbox.domain.activity.entity.ActivityLog.EventType;
import com.blackbox.domain.activity.service.ActivityLogService;
import com.blackbox.domain.project.entity.Project;
import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.task.dto.TaskDto;
import com.blackbox.domain.task.entity.Task;
import com.blackbox.domain.task.entity.Task.Status;
import com.blackbox.domain.task.repository.TaskRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.domain.user.repository.UserRepository;
import com.blackbox.global.exception.BusinessException;
import com.blackbox.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public TaskDto.Response createTask(Long projectId, Long userId, TaskDto.CreateRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Task task = Task.builder()
                .project(project)
                .title(req.getTitle())
                .description(req.getDescription())
                .priority(req.getPriority() != null ? req.getPriority() : Task.Priority.MEDIUM)
                .tag(req.getTag())
                .dueDate(req.getDueDate())
                .createdBy(user)
                .assignees(new HashSet<>())
                .build();

        if (req.getAssigneeIds() != null) {
            List<User> assignees = userRepository.findAllById(req.getAssigneeIds());
            task.getAssignees().addAll(assignees);
        }

        task = taskRepository.save(task);

        activityLogService.logTaskEvent(
                project, user, task.getId(), EventType.TASK_CREATED,
                Map.of("title", task.getTitle())
        );

        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskDto.Response> getTasks(Long projectId) {
        return taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDto.Response updateTask(Long taskId, Long userId, TaskDto.UpdateRequest req) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        if (req.getPriority() != null) task.setPriority(req.getPriority());
        task.setTag(req.getTag());
        task.setDueDate(req.getDueDate());

        if (req.getAssigneeIds() != null) {
            List<User> assignees = userRepository.findAllById(req.getAssigneeIds());
            task.setAssignees(new HashSet<>(assignees));
        }

        User user = userRepository.findById(userId).orElseThrow();
        activityLogService.logTaskEvent(
                task.getProject(), user, task.getId(), EventType.TASK_UPDATED,
                Map.of("title", task.getTitle())
        );

        return toResponse(task);
    }

    @Transactional
    public void updateStatus(Long taskId, Long userId, TaskDto.StatusUpdateRequest req) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        Status oldStatus = task.getStatus();
        task.setStatus(req.getStatus());

        if (req.getStatus() == Status.DONE && oldStatus != Status.DONE) {
            task.setCompletedAt(Instant.now());
        } else if (req.getStatus() != Status.DONE) {
            task.setCompletedAt(null);
        }

        User user = userRepository.findById(userId).orElseThrow();
        activityLogService.logTaskEvent(
                task.getProject(), user, task.getId(), EventType.TASK_STATUS_CHANGED,
                Map.of("oldStatus", oldStatus.name(), "newStatus", task.getStatus().name())
        );
    }

    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        
        User user = userRepository.findById(userId).orElseThrow();
        activityLogService.logTaskEvent(
                task.getProject(), user, task.getId(), EventType.TASK_DELETED,
                Map.of("title", task.getTitle())
        );

        taskRepository.delete(task);
    }

    private TaskDto.Response toResponse(Task task) {
        return TaskDto.Response.builder()
                .id(task.getId())
                .projectId(task.getProject().getId())
                .meetingId(task.getMeeting() != null ? task.getMeeting().getId() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .tag(task.getTag())
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .createdById(task.getCreatedBy().getId())
                .assigneeIds(task.getAssignees().stream().map(User::getId).collect(Collectors.toList()))
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
