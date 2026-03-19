package com.tms.backend.rolePermission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tms.backend.dto.PermissionItemDTO;
import com.tms.backend.dto.RolePermissionCreateDTO;
import com.tms.backend.dto.RolePermissionGroupDTO;
import com.tms.backend.role.Role;
import com.tms.backend.role.RoleConstants;
import com.tms.backend.role.RoleRepository;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class RolePermissionService {

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    private static final Map<String, Set<Permission>> ROLE_PERMISSIONS = Map.of(
            RoleConstants.PM, getPermissionsByCategories(
                    PermissionCategory.PROJECT,
                    PermissionCategory.PROJECT_TEMPLATES,
                    PermissionCategory.TRANSLATION_MEMORIES,
                    PermissionCategory.TERM_BASE,
                    PermissionCategory.USERS,
                    PermissionCategory.CLIENT_DOMAIN_SUBDOMAIN,
                    PermissionCategory.VENDORS,
                    PermissionCategory.ANALYTICS
            ),
            RoleConstants.LINGUIST, EnumSet.of(
                    Permission.TB_UPDATE_TERM,
                    Permission.TM_UPDATE_TRANSLATION,
                    Permission.JOB_REJECT,
                    Permission.MT_ENABLE
            ),
            RoleConstants.GUEST, EnumSet.of(
                    Permission.PROJECT_VIEW,
                    Permission.JOB_PROVIDER_VIEW,
                    Permission.JOB_EDITOR_VIEW,
                    Permission.TM_VIEW,
                    Permission.TM_UPDATE,
                    Permission.TM_EXPORT,
                    Permission.TM_IMPORT,
                    Permission.TB_VIEW,
                    Permission.TB_UPDATE,
                    Permission.TB_EXPORT,
                    Permission.TB_IMPORT,
                    Permission.TB_APPROVE_TERMS
            )
    );

    private static Set<Permission> getPermissionsByCategories(PermissionCategory... categories) {
        Set<PermissionCategory> categorySet = EnumSet.copyOf(Arrays.asList(categories));
        EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
        for (Permission p : Permission.values()) {
            if (categorySet.contains(p.getCategory())) {
                permissions.add(p);
            }
        }
        return permissions;
    }

    @Transactional
    public List<RolePermission> createRolePermissions(RolePermissionCreateDTO dto) {
        Role role = roleRepository.findById(dto.roleId())
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        Set<Permission> availablePermissions = ROLE_PERMISSIONS.get(role.getName());
        if (availablePermissions == null) {
            throw new IllegalArgumentException("No permissions defined for role: " + role.getName());
        }

        Map<Permission, Boolean> permissionToggles = dto.permissions();

        List<RolePermission> rolePermissions = new ArrayList<>();
        for (Permission permission : availablePermissions) {
            RolePermission rp = new RolePermission();
            rp.setRole(role);
            rp.setCategory(permission.getCategory());
            rp.setPermission(permission);
            rp.setActive(permissionToggles != null && Boolean.TRUE.equals(permissionToggles.get(permission)));
            rolePermissions.add(rp);
        }

        return rolePermissionRepository.saveAll(rolePermissions);
    }

    public Map<String, RolePermissionGroupDTO> getAllRolePermissions() {
        return groupByRole(rolePermissionRepository.findAll());
    }

    public RolePermissionGroupDTO getRolePermissionsByRoleId(Long roleId) {
        List<RolePermission> permissions = rolePermissionRepository.findByRoleId(roleId);
        if (permissions.isEmpty()) {
            return new RolePermissionGroupDTO(roleId, Map.of());
        }
        return toGroupDTO(permissions.get(0).getRole().getId(), permissions);
    }

    private Map<String, RolePermissionGroupDTO> groupByRole(List<RolePermission> all) {
        Map<String, RolePermissionGroupDTO> result = new LinkedHashMap<>();
        Map<String, List<RolePermission>> byRole = all.stream()
                .collect(Collectors.groupingBy(rp -> rp.getRole().getName(), LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<RolePermission>> entry : byRole.entrySet()) {
            Long roleId = entry.getValue().get(0).getRole().getId();
            result.put(entry.getKey(), toGroupDTO(roleId, entry.getValue()));
        }
        return result;
    }

    private RolePermissionGroupDTO toGroupDTO(Long roleId, List<RolePermission> permissions) {
        Map<PermissionCategory, List<PermissionItemDTO>> categories = permissions.stream()
                .collect(Collectors.groupingBy(
                        RolePermission::getCategory,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toItemDTO, Collectors.toList())
                ));
        return new RolePermissionGroupDTO(roleId, categories);
    }

    private PermissionItemDTO toItemDTO(RolePermission rp) {
        return new PermissionItemDTO(
                rp.getId(),
                rp.getPermission().name(),
                rp.getPermission().getDisplayName(),
                rp.getPermission().getDescription(),
                rp.isActive()
        );
    }

    public Map<PermissionCategory, List<PermissionItemDTO>> getAvailablePermissions(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        Set<Permission> availablePermissions = ROLE_PERMISSIONS.get(role.getName());
        if (availablePermissions == null) {
            return Map.of();
        }

        return availablePermissions.stream()
                .collect(Collectors.groupingBy(
                        Permission::getCategory,
                        LinkedHashMap::new,
                        Collectors.mapping(p -> new PermissionItemDTO(
                                null,
                                p.name(),
                                p.getDisplayName(),
                                p.getDescription(),
                                false
                        ), Collectors.toList())
                ));
    }

    @Transactional
    public void deleteRolePermissionsByRoleId(Long roleId) {
        rolePermissionRepository.deleteByRoleId(roleId);
    }

    @PostConstruct
    @Transactional
    public void autoPopulateRolePermissions() {
        for (Map.Entry<String, Set<Permission>> entry : ROLE_PERMISSIONS.entrySet()) {
            List<Role> roles = roleRepository.findByName(entry.getKey());
            if (roles.isEmpty()) {
                continue;
            }
            Role role = roles.get(0);
            List<RolePermission> existing = rolePermissionRepository.findByRoleId(role.getId());
            if (!existing.isEmpty()) {
                continue;
            }

            List<RolePermission> rolePermissions = new ArrayList<>();
            for (Permission permission : entry.getValue()) {
                RolePermission rp = new RolePermission();
                rp.setRole(role);
                rp.setCategory(permission.getCategory());
                rp.setPermission(permission);
                rp.setActive(true);
                rolePermissions.add(rp);
            }
            rolePermissionRepository.saveAll(rolePermissions);
        }
    }
}
