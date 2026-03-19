package com.tms.backend.dto;

import java.util.List;
import java.util.Map;

import com.tms.backend.rolePermission.PermissionCategory;

public record RolePermissionGroupDTO(
    Long roleId,
    Map<PermissionCategory, List<PermissionItemDTO>> categories
) {}
