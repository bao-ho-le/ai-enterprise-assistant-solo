package com.enterprise.aiassistant.backend.admin.role.dto;

import com.enterprise.aiassistant.backend.user.entity.Permission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRolePermissionsRequest {

    // Ghi đè toàn bộ permission của role. Chỉ nhận giá trị nằm trong Permission catalog.
    private List<Permission> permissions;
}
