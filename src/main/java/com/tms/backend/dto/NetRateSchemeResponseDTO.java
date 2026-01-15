package com.tms.backend.dto;

import java.util.List;

public record NetRateSchemeResponseDTO(
    Long id,
    String name,
    Long projectId,
    List<WorkflowStepRateResponseDTO> workflowStepRates
) {}