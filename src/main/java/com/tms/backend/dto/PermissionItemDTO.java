package com.tms.backend.dto;

public record PermissionItemDTO(
    Long id,
    String permission,
    String displayName,
    String description,
    boolean isActive
) {}
