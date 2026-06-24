package com.tms.backend.dto;

public record PriceListLanguagePairResponseDTO(
    Long id,
    String sourceLanguage,
    String targetLanguage,
    Double price
) {}
