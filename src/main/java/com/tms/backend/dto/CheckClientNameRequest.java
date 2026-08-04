package com.tms.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckClientNameRequest(
    @NotBlank String name,
    Long excludeId
) {}
