package com.tms.backend.projectTmAssignment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTmSizingRepository extends JpaRepository<ProjectTmSizing, Long> {

    Optional<ProjectTmSizing> findFirstByProject_IdAndSourceLangAndTargetLangAndStatusOrderByCreatedAtDesc(
        Long projectId,
        String sourceLang,
        String targetLang,
        String status
    );

    List<ProjectTmSizing> findByStatus(String status);
}
