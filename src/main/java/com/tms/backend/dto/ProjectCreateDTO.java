package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProjectCreateDTO(
    @NotBlank(message = "Project name is required")
    @Size(max = 255, message = "Project name must not exceed 255 characters")
    String name,
    
    @Future(message = "Due date must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dueDate,
    
    @NotBlank(message = "Source language is required")
    @Size(max = 10, message = "Source language code too long")
    String sourceLang,
    
    @NotEmpty(message = "At least one target language is required")
    @Size(max = 3, message = "Too many target languages")
    Set<@NotBlank @Size(max = 10) String> targetLang,
    
    Long machineTranslationId,
    
    Long businessUnitId,
    
    @Size(max = 10, message = "Purchase order must not exceed 10 characters")
    String purchaseOrder,
    String type,
    Long clientId,
    @Size(max = 200, message = "Note must not exceed 200 characters")
    String note,
    Long costCenterId,
    Long domainId,
    Long subdomainId,
    Set<@Positive Long> workflowSteps,
    String owner,
    Set<String> automationRules
) {}
