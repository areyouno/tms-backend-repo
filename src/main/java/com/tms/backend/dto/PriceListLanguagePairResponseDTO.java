package com.tms.backend.dto;

import java.util.List;

public record PriceListLanguagePairResponseDTO(
    Long id,
    String sourceLanguage,
    String targetLanguage,
    Double minPrice,
    List<PriceListWorkflowStepPriceResponseDTO> workflowStepPrices
) {}
