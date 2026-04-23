package com.tms.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.tms.backend.priceList.BillingUnit;
import com.tms.backend.quote.QuoteType;

public record QuoteCreateDTO(
    String name,
    QuoteType type,
    Long providerId,
    Long priceListId,
    Long currencyId,
    BillingUnit billingUnit,
    Long netRateSchemeId,
    Long jobAnalysisId,
    List<WorkflowStepEntry> workflowSteps
) {
    public record WorkflowStepEntry(
        Long workflowStepId,
        Long netWords,
        BigDecimal price
    ) {}
}
