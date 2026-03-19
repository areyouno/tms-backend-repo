package com.tms.backend.dto;

public record PermissionTemplateDTO(
    String permission,
    String displayName,
    String description,
    boolean isActive
) {}
