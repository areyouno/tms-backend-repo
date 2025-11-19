package com.tms.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkflowStepCreateDTO(
    @NotBlank(message = "Name is required")
    String name,
    
    @NotBlank(message = "Abbreviation is required")
    String abbreviation,
    
    @NotNull(message = "Display order is required")
    Integer displayOrder,

    Boolean isLQA  // Optional, defaults to false
) {}
