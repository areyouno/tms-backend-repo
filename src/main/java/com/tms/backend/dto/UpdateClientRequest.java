package com.tms.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateClientRequest(
    @NotBlank String name,
    String externalId
) {}
