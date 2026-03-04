package com.tms.backend.dto;

import com.tms.backend.priceList.BillingUnit;

public record PriceListUpdateDTO(
    String name,
    Long currencyId,
    BillingUnit billingUnit
) {}
