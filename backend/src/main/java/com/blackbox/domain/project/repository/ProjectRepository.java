package com.blackbox.domain.project.repository;

import com.blackbox.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByInviteCode(String inviteCode);

    @Query("""
            SELECT p FROM Project p
            JOIN p.members m
            WHERE m.user.id = :userId AND p.active = true
            """)
    List<Project> findAllByMemberUserId(@Param("userId") Long userId);
}
