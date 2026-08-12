package com.enterprise.aiassistant.backend.admin.role.service;

import com.enterprise.aiassistant.backend.admin.role.dto.RolePermissionResponse;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.enums.Role;
import com.enterprise.aiassistant.backend.user.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

// Permission là system-defined (enum), admin chỉ gán/bỏ gán cho 4 base role. Không có permission
// riêng cho "quản lý role" trong catalog, nên chốt bằng role ADMIN ở service layer chứ không
// chỉ ở filter chain.
@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final RolePermissionService rolePermissionService;

    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<RolePermissionResponse> getRoles() {

        requireAdmin();

        return rolePermissionService.getAllRolePermissions().entrySet().stream()
                .map(entry -> RolePermissionResponse.builder()
                        .role(entry.getKey().name())
                        .permissions(entry.getValue().stream().map(Permission::name).sorted().toList())
                        .build())
                .toList();
    }

    // Catalog đầy đủ để UI dựng checkbox — không có API tạo permission mới.
    @Transactional(readOnly = true)
    public List<String> getPermissionCatalog() {

        requireAdmin();

        return Arrays.stream(Permission.values()).map(Permission::name).toList();
    }

    @Transactional
    public RolePermissionResponse updateRolePermissions(Role role, List<Permission> permissions) {

        requireAdmin();

        return RolePermissionResponse.builder()
                .role(role.name())
                .permissions(rolePermissionService.replacePermissions(role, permissions).stream()
                        .map(Permission::name)
                        .sorted()
                        .toList())
                .build();
    }

    // Helper

    private void requireAdmin() {

        if (currentUserService.getCurrentPrincipal().getRole() != Role.ADMIN) {
            throw new AuthorizationException(ErrorCode.PERMISSION_DENIED);
        }
    }
}
