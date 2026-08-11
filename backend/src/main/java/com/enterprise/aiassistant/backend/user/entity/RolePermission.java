package com.enterprise.aiassistant.backend.user.entity;

import jakarta.persistence.*;
import lombok.*;

// Bảng gán Permission (system-defined) cho Role (system-defined).
// Chỉ mapping là dữ liệu, không tạo role/permission mới lúc runtime.
@Entity
@Table(
        name = "role_permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_role_permission",
                columnNames = {"role", "permission"}
        ),
        indexes = @Index(name = "idx_role_permission_role", columnList = "role")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Permission permission;
}
