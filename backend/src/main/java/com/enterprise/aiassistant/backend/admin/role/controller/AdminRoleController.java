package com.enterprise.aiassistant.backend.admin.role.controller;

import com.enterprise.aiassistant.backend.admin.role.dto.RolePermissionResponse;
import com.enterprise.aiassistant.backend.admin.role.dto.UpdateRolePermissionsRequest;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.user.entity.Permission;
import com.enterprise.aiassistant.backend.user.entity.Role;
import com.enterprise.aiassistant.backend.user.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

// Permission là system-defined (enum), admin chỉ gán/bỏ gán cho 4 base role.
@RestController
@RequestMapping("${api.prefix}/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleController {

    private final RolePermissionService rolePermissionService;

    private final CurrentUserService currentUserService;

    @GetMapping
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
    @GetMapping("/permissions")
    public List<String> getPermissionCatalog() {

        requireAdmin();

        return Arrays.stream(Permission.values()).map(Permission::name).toList();
    }

    @PutMapping("/{role}/permissions")
    public ResponseEntity<RolePermissionResponse> updateRolePermissions(
            @PathVariable Role role,
            @RequestBody UpdateRolePermissionsRequest request
    ) {

        requireAdmin();

        List<Permission> permissions = request == null ? null : request.getPermissions();

        return ResponseEntity.ok(
                RolePermissionResponse.builder()
                        .role(role.name())
                        .permissions(rolePermissionService.replacePermissions(role, permissions).stream()
                                .map(Permission::name)
                                .sorted()
                                .toList())
                        .build()
        );
    }

    // Không có permission riêng cho "quản lý role" trong catalog, nên chốt bằng role ADMIN
    // ở cả service layer chứ không chỉ ở filter chain.
    private void requireAdmin() {

        if (currentUserService.getCurrentPrincipal().getRole() != Role.ADMIN) {
            throw new AuthorizationException(ErrorCode.PERMISSION_DENIED);
        }
    }
}
