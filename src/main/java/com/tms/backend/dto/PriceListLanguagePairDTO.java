package com.tms.backend.dto;

public record PriceListLanguagePairDTO(
    String sourceLanguage,
    String targetLanguage,
    Double price
) {}
