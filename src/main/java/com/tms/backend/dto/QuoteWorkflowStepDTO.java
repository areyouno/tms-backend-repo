package com.tms.backend.dto;

import java.math.BigDecimal;

import com.tms.backend.quote.QuoteWorkflowStep;

public record QuoteWorkflowStepDTO(
    Long id,
    Long workflowStepId,
    String workflowStepName,
    Long netWords,
    BigDecimal price
) {
    public static QuoteWorkflowStepDTO fromEntity(QuoteWorkflowStep step) {
        return new QuoteWorkflowStepDTO(
            step.getId(),
            step.getWorkflowStep() != null ? step.getWorkflowStep().getId() : null,
            step.getWorkflowStep() != null ? step.getWorkflowStep().getName() : null,
            step.getNetWords(),
            step.getPrice()
        );
    }
}
