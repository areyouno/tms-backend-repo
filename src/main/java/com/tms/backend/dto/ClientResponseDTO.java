package com.tms.backend.dto;

public record ClientResponseDTO(
    Long id,
    String uuid,
    String name,
    String externalId,
    boolean active,
    Long netRateSchemeId,
    String netRateSchemeName
) {}
