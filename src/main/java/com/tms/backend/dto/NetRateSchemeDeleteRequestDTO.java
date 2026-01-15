package com.tms.backend.dto;

import java.util.List;

public record NetRateSchemeDeleteRequestDTO(
    List<Long> ids
) {}

