package com.tms.backend.dto;

import java.util.List;

public record NetRateSchemeWfDTO(
    Long workflowStepId,
    List<MatchTypeRateDTO> matchTypeRates
) {}
