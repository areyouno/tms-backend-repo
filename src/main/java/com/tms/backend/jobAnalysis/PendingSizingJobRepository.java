package com.tms.backend.jobAnalysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingSizingJobRepository extends JpaRepository<PendingSizingJob, String> {
    List<PendingSizingJob> findByProjectId(Long projectId);
}
