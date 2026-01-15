package com.tms.backend.dto;

import com.tms.backend.netRateScheme.MatchType;

public record MatchTypeRateResponseDTO(
    MatchType matchType,
    Long transMemoryPercent,
    Long machineTransPercent,
    Long nonTranslatablePercent,
    Long internalFuzziesPercent
) {}
