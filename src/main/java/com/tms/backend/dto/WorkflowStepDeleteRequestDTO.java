package com.tms.backend.dto;

import java.util.List;

public record WorkflowStepDeleteRequestDTO(
    List<Long> ids
) {}
