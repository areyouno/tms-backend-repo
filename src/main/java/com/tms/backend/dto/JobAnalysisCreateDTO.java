package com.tms.backend.dto;

import com.tms.backend.jobAnalysis.JobAnalysisType;

public record JobAnalysisCreateDTO(
    Long jobId,
    Long workflowStepId,
    JobAnalysisType type,
    String name
) {}
