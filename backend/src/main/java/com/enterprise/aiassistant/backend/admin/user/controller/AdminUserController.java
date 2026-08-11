package com.enterprise.aiassistant.backend.admin.user.controller;

import com.enterprise.aiassistant.backend.admin.user.dto.request.AdminCreateUserRequest;
import com.enterprise.aiassistant.backend.admin.user.dto.request.AdminUpdateUserRequest;
import com.enterprise.aiassistant.backend.admin.user.dto.request.AssignRoleRequest;
import com.enterprise.aiassistant.backend.admin.user.dto.request.ChangeDepartmentRequest;
import com.enterprise.aiassistant.backend.admin.user.service.AdminUserService;
import com.enterprise.aiassistant.backend.user.dto.response.UserResponse;
import com.enterprise.aiassistant.backend.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Page<UserResponse> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean enabled,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return adminUserService.getUsers(keyword, role, departmentId, enabled, pageable);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.getUserDetail(userId));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.ok(adminUserService.createUser(request));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @RequestBody AdminUpdateUserRequest request
    ) {
        return ResponseEntity.ok(adminUserService.updateUser(userId, request));
    }

    @PutMapping("/{userId}/enable")
    public ResponseEntity<UserResponse> enableUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.setEnabled(userId, true));
    }

    @PutMapping("/{userId}/disable")
    public ResponseEntity<UserResponse> disableUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.setEnabled(userId, false));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable Long userId,
            @RequestBody AssignRoleRequest request
    ) {
        return ResponseEntity.ok(adminUserService.assignRole(userId, request));
    }

    @PutMapping("/{userId}/department")
    public ResponseEntity<UserResponse> changeDepartment(
            @PathVariable Long userId,
            @RequestBody ChangeDepartmentRequest request
    ) {
        return ResponseEntity.ok(adminUserService.changeDepartment(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminUserService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
