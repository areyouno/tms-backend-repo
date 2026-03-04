package com.tms.backend.dto;

import java.util.List;

public record PriceListDeleteRequestDTO(
    List<Long> ids
) {}
