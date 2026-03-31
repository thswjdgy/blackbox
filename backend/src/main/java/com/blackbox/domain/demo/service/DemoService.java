package com.blackbox.domain.demo.service;

import com.blackbox.domain.project.entity.Project;
import com.blackbox.domain.project.repository.ProjectRepository;
import com.blackbox.domain.task.entity.Task;
import com.blackbox.domain.task.repository.TaskRepository;
import com.blackbox.domain.user.entity.User;
import com.blackbox.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void seedData() {
        log.info("Seeding demo data...");

        // 1. Create a dummy professor
        User prof = userRepository.findByEmail("prof@demo.com").orElseGet(() -> {
            User p = User.builder()
                    .email("prof@demo.com")
                    .name("Professor")
                    .passwordHash(passwordEncoder.encode("prof1234"))
                    .role(User.Role.PROFESSOR)
                    .dataCollectionConsent(true)
                    .build();
            return userRepository.save(p);
        });

        // 2. Create Projects & Tasks
        for (int i = 1; i <= 3; i++) {
            final int pIdx = i;
            Project proj = projectRepository.findAll().stream()
                    .filter(pr -> pr.getName().equals("Demo Project " + pIdx))
                    .findFirst()
                    .orElseGet(() -> {
                        Project newP = Project.builder()
                                .name("Demo Project " + pIdx)
                                .description("This is an auto-generated demo project.")
                                .inviteCode(UUID.randomUUID().toString().substring(0, 8))
                                .active(true)
                                .build();
                        return projectRepository.save(newP);
                    });

            // Add some tasks
            for (int t = 1; t <= 5; t++) {
                final int tIdx = t;
                boolean exists = taskRepository.findByProjectIdOrderByCreatedAtDesc(proj.getId()).stream()
                        .anyMatch(task -> task.getTitle().equals("Demo Task " + tIdx + " for Project " + pIdx));
                if (!exists) {
                    Task task = Task.builder()
                            .project(proj)
                            .title("Demo Task " + tIdx + " for Project " + pIdx)
                            .description("Auto generated task.")
                            .createdBy(prof)
                            .status(tIdx % 2 == 0 ? Task.Status.DONE : Task.Status.IN_PROGRESS)
                            .priority(Task.Priority.HIGH)
                            .build();
                    taskRepository.save(task);
                }
            }
        }
        log.info("Seeding completed.");
    }
}
