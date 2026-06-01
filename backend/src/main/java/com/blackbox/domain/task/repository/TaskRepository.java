package com.blackbox.domain.task.repository;

import com.blackbox.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<Task> findByProjectIdAndStatusNotAndDueDateBetween(
            Long projectId, Task.Status status, Instant from, Instant to);
}
