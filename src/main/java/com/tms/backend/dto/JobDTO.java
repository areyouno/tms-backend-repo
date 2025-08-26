package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.Set;


public record JobDTO(
    Long id,
    String status,
    Set<String> targetLangs,
    String provider,
    LocalDateTime dueDate,
    Long jobOwnerId,
    String fileName,
    String contentType,
    Long projectId
) 
{}
