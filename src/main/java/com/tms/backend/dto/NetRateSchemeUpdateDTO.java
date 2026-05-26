package com.tms.backend.dto;

import java.util.List;

public record NetRateSchemeUpdateDTO(
    String name,
    List<MatchTypeRateDTO> matchTypeRates
) {}
