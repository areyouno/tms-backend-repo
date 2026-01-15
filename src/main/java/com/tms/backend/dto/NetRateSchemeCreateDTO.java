package com.tms.backend.dto;

import java.util.List;

public record NetRateSchemeCreateDTO(
    String name,
    Long projectId,
    List<NetRateSchemeWfDTO> workflowSteps
)
{}
