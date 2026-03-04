package com.tms.backend.dto;

import java.util.Set;

import com.tms.backend.jobAnalysis.JobAnalysisType;

public record JobAnalysisCreateDTO(
    Long jobId,
    Long workflowStepId,
    JobAnalysisType type,
    String name,
    String sourceLang,
    Set<String> languages
) {}
