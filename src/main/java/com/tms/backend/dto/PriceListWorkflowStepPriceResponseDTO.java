package com.tms.backend.dto;

public record PriceListWorkflowStepPriceResponseDTO(
    Long workflowStepId,
    String workflowStepName,
    Double price
) {}
