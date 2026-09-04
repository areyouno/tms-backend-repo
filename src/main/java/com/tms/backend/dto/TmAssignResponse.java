package com.tms.backend.dto;

public record TmAssignResponse(
    Long tmId,
    String name,
    Long projectId,
    String assignedUserId,
    String workflowStage,
    Boolean wasExisting
) {}
