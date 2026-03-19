package com.tms.backend.rolePermission;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.RolePermissionCreateDTO;
import com.tms.backend.dto.RolePermissionGroupDTO;

@RestController
@RequestMapping("/api/role-permissions")
@PreAuthorize("hasAuthority('administrator')")
public class RolePermissionController {

    @Autowired
    private RolePermissionService rolePermissionService;

    @PostMapping("/create")
    public ResponseEntity<List<RolePermission>> createRolePermissions(@RequestBody RolePermissionCreateDTO dto) {
        List<RolePermission> created = rolePermissionService.createRolePermissions(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/all")
    public Map<String, RolePermissionGroupDTO> getAllRolePermissions() {
        return rolePermissionService.getAllRolePermissions();
    }

    @GetMapping("/role/{roleId}")
    public RolePermissionGroupDTO getRolePermissionsByRoleId(@PathVariable Long roleId) {
        return rolePermissionService.getRolePermissionsByRoleId(roleId);
    }

    @GetMapping("/permissions")
    public Map<PermissionCategory, List<com.tms.backend.dto.PermissionTemplateDTO>> getAllPermissions() {
        return rolePermissionService.getAllPermissions();
    }

    @GetMapping("/available/{roleId}")
    public Map<PermissionCategory, List<com.tms.backend.dto.PermissionItemDTO>> getAvailablePermissions(@PathVariable Long roleId) {
        return rolePermissionService.getAvailablePermissions(roleId);
    }

    @DeleteMapping("/role/{roleId}")
    public ResponseEntity<Void> deleteRolePermissionsByRoleId(@PathVariable Long roleId) {
        rolePermissionService.deleteRolePermissionsByRoleId(roleId);
        return ResponseEntity.noContent().build();
    }
}
