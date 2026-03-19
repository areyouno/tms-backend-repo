package com.tms.backend.dto;

import java.util.Map;

import com.tms.backend.rolePermission.Permission;

public record RolePermissionUpdateDTO(
    Long roleId,
    Map<Permission, Boolean> permissions
) {}
