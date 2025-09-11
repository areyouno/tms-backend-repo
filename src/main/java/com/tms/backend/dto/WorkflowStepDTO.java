package com.tms.backend.dto;

public record WorkflowStepDTO(
    Long id,
    String name,
    Integer displayOrder
) {}
