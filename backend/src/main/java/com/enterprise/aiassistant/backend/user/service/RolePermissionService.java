package com.enterprise.aiassistant.backend.user.service;

import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.enums.Role;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface RolePermissionService {

    // Seed mapping mặc định cho các role chưa có bản ghi nào. Idempotent.
    void seedDefaultRolePermissionsIfMissing();

    Set<Permission> getPermissions(Role role);

    Map<Role, Set<Permission>> getAllRolePermissions();

    // Ghi đè toàn bộ permission của 1 role bằng danh sách mới (system-defined only).
    Set<Permission> replacePermissions(Role role, Collection<Permission> permissions);
}
