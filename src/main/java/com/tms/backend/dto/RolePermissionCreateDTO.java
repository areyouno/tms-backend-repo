package com.tms.backend.dto;

import java.util.Map;

import com.tms.backend.rolePermission.Permission;

public record RolePermissionCreateDTO(
    Long roleId,
    Map<Permission, Boolean> permissions
) {}
