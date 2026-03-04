package com.tms.backend.dto;

import java.util.List;

public record PriceListLanguagePairDTO(
    String sourceLanguage,
    String targetLanguage,
    Double minPrice,
    List<PriceListWorkflowStepPriceDTO> workflowStepPrices
) {}
