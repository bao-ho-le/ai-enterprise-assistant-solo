package com.enterprise.aiassistant.backend.user.service;

import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.enums.Role;
import com.enterprise.aiassistant.backend.user.entity.RolePermission;
import com.enterprise.aiassistant.backend.user.helper.RolePermissionHelper;
import com.enterprise.aiassistant.backend.user.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    private final RolePermissionHelper rolePermissionHelper;

    // Mapping đọc trên mọi request đã authenticate nên cache lại, chỉ invalidate khi admin sửa.
    private final Map<Role, Set<Permission>> permissionCache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void seedDefaultRolePermissionsIfMissing() {

        for (Role role : Role.values()) {

            if (!rolePermissionRepository.findByRole(role).isEmpty()) {
                continue;
            }

            List<RolePermission> grants = rolePermissionHelper.defaultPermissionsOf(role).stream()
                    .map(permission -> RolePermission.builder()
                            .role(role)
                            .permission(permission)
                            .build())
                    .toList();

            rolePermissionRepository.saveAll(grants);
        }

        permissionCache.clear();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Permission> getPermissions(Role role) {

        rolePermissionHelper.validateRole(role);

        return permissionCache.computeIfAbsent(role, this::loadPermissions);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Role, Set<Permission>> getAllRolePermissions() {

        Map<Role, Set<Permission>> result = new EnumMap<>(Role.class);

        for (Role role : Role.values()) {
            result.put(role, getPermissions(role));
        }

        return result;
    }

    @Override
    @Transactional
    public Set<Permission> replacePermissions(Role role, Collection<Permission> permissions) {

        rolePermissionHelper.validateUpdatePermissions(role, permissions);

        rolePermissionRepository.deleteByRole(role);

        Set<Permission> distinctPermissions = new LinkedHashSet<>(permissions);

        rolePermissionRepository.saveAll(
                distinctPermissions.stream()
                        .map(permission -> RolePermission.builder()
                                .role(role)
                                .permission(permission)
                                .build())
                        .toList()
        );

        permissionCache.remove(role);

        return distinctPermissions;
    }

    private Set<Permission> loadPermissions(Role role) {

        Set<Permission> permissions = EnumSet.noneOf(Permission.class);

        rolePermissionRepository.findByRole(role)
                .forEach(grant -> permissions.add(grant.getPermission()));

        // Chưa seed (DB trống) thì rơi về mapping mặc định để hệ thống không bị khoá cứng.
        return permissions.isEmpty()
                ? rolePermissionHelper.defaultPermissionsOf(role)
                : permissions;
    }
}
