package com.enterprise.aiassistant.backend.admin.user.dto.request;

import com.enterprise.aiassistant.backend.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleRequest {

    private Role role;
}
