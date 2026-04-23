package com.tms.backend.jobAnalysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobAnalysisRepository extends JpaRepository<JobAnalysis, Long> {
    List<JobAnalysis> findByProjectId(Long projectId);
}
