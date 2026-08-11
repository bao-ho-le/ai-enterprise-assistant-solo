package com.enterprise.aiassistant.backend.user.mapper;

import com.enterprise.aiassistant.backend.user.dto.response.UserResponse;
import com.enterprise.aiassistant.backend.user.entity.Permission;
import com.enterprise.aiassistant.backend.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    // Dùng cho danh sách: bỏ permissions để không lặp lại cùng một tập cho mọi dòng.
    public UserResponse toResponse(User user) {
        return build(user, null);
    }

    public UserResponse toResponse(User user, Set<Permission> permissions) {
        return build(user, permissions);
    }

    private UserResponse build(User user, Set<Permission> permissions) {

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .permissions(permissions == null
                        ? null
                        : permissions.stream().map(Permission::name).collect(Collectors.toSet()))
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
