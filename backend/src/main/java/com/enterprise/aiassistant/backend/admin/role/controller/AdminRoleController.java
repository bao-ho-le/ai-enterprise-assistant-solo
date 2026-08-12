package com.enterprise.aiassistant.backend.admin.role.controller;

import com.enterprise.aiassistant.backend.admin.role.dto.RolePermissionResponse;
import com.enterprise.aiassistant.backend.admin.role.dto.UpdateRolePermissionsRequest;
import com.enterprise.aiassistant.backend.admin.role.service.AdminRoleService;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Chỉ expose use-case admin, logic nghiệp vụ vẫn nằm trong AdminRoleService.
@RestController
@RequestMapping("${api.prefix}/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @GetMapping
    public List<RolePermissionResponse> getRoles() {
        return adminRoleService.getRoles();
    }

    @GetMapping("/permissions")
    public List<String> getPermissionCatalog() {
        return adminRoleService.getPermissionCatalog();
    }

    @PutMapping("/{role}/permissions")
    public ResponseEntity<RolePermissionResponse> updateRolePermissions(
            @PathVariable Role role,
            @RequestBody UpdateRolePermissionsRequest request
    ) {
        List<Permission> permissions = request == null ? null : request.getPermissions();

        return ResponseEntity.ok(adminRoleService.updateRolePermissions(role, permissions));
    }
}
