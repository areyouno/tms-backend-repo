package com.tms.backend.dto;

import java.util.List;

public record NetRateSchemeCreateDTO(
    String name,
    boolean isDefault,
    List<NetRateSchemeWfDTO> workflowSteps
)
{}
