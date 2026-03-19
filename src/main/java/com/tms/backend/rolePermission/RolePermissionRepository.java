package com.tms.backend.rolePermission;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleId(Long roleId);

    void deleteByRoleId(Long roleId);
}
