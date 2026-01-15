package com.tms.backend.dto;

import java.util.List;

public record WorkflowStepRateResponseDTO(
    Long workflowStepId,
    List<MatchTypeRateResponseDTO> matchTypeRates
) {}
