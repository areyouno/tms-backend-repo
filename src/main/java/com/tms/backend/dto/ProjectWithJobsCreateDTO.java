package com.tms.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ProjectWithJobsCreateDTO(
    @Valid
    @NotNull(message = "Project details are required")
    ProjectCreateDTO project,

    List<JobWorkflowStepDTO> workflowSteps
) {}
