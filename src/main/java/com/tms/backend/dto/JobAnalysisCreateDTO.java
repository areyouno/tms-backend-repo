package com.tms.backend.dto;

import java.util.List;

import com.tms.backend.jobAnalysis.JobAnalysisType;

public record JobAnalysisCreateDTO(
    List<Long> jobIds,
    Long workflowStepId,
    JobAnalysisType type,
    String name
) {}
