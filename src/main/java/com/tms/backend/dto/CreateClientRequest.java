package com.tms.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateClientRequest(
    @NotBlank String name,
    String externalId
) {}
