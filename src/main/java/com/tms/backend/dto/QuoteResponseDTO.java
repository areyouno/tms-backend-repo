package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.tms.backend.priceList.BillingUnit;
import com.tms.backend.quote.Quote;
import com.tms.backend.quote.QuoteType;

public record QuoteResponseDTO(
    Long id,
    String name,
    QuoteType type,
    String sourceLanguage,
    String targetLanguage,
    Long providerId,
    String providerUsername,
    Long priceListId,
    String priceListName,
    Long currencyId,
    String currencyCode,
    BillingUnit billingUnit,
    Long netRateSchemeId,
    String netRateSchemeName,
    Long jobAnalysisId,
    String jobAnalysisName,
    List<QuoteWorkflowStepDTO> workflowSteps,
    LocalDateTime createDate
) {
    public static QuoteResponseDTO fromEntity(Quote quote) {
        return new QuoteResponseDTO(
            quote.getId(),
            quote.getName(),
            quote.getType(),
            quote.getSourceLanguage(),
            quote.getTargetLanguage(),
            quote.getProvider() != null ? quote.getProvider().getId() : null,
            quote.getProvider() != null ? quote.getProvider().getUsername() : null,
            quote.getPriceList() != null ? quote.getPriceList().getId() : null,
            quote.getPriceList() != null ? quote.getPriceList().getName() : null,
            quote.getCurrency() != null ? quote.getCurrency().getId() : null,
            quote.getCurrency() != null ? quote.getCurrency().getCode() : null,
            quote.getBillingUnit(),
            quote.getNetRateScheme() != null ? quote.getNetRateScheme().getId() : null,
            quote.getNetRateScheme() != null ? quote.getNetRateScheme().getName() : null,
            quote.getJobAnalysis() != null ? quote.getJobAnalysis().getId() : null,
            quote.getJobAnalysis() != null ? quote.getJobAnalysis().getName() : null,
            quote.getWorkflowSteps() != null
                ? quote.getWorkflowSteps().stream().map(QuoteWorkflowStepDTO::fromEntity).collect(Collectors.toList())
                : List.of(),
            quote.getCreateDate()
        );
    }
}
