package com.tms.backend.dto;

public record TmTemplateAssignResponse(
    Long tmId,
    String name,
    Long templateTmId,
    String assignedUserId,
    String workflowStage,
    Boolean wasExisting
) {}
