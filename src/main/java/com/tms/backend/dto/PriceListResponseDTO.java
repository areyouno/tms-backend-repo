package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.tms.backend.priceList.BillingUnit;

public record PriceListResponseDTO(
    Long id,
    String name,
    Long currencyId,
    String currencyCode,
    String currencyName,
    BillingUnit billingUnit,
    LocalDateTime createDate,
    Long createdById,
    String createdByName,
    List<PriceListLanguagePairResponseDTO> languagePairs
) {}
