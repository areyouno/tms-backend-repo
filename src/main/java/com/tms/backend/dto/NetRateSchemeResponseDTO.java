package com.tms.backend.dto;

import java.util.List;

public record NetRateSchemeResponseDTO(
    Long id,
    String name,
    boolean isDefault,
    List<WorkflowStepRateResponseDTO> workflowStepRates
) {}