package com.tms.backend.dto;

import java.util.List;

import com.tms.backend.priceList.BillingUnit;

public record PriceListCreateDTO(
    String name,
    Long currencyId,
    BillingUnit billingUnit,
    List<PriceListLanguagePairDTO> languagePairs
) {}
