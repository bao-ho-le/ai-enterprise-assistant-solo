package com.enterprise.aiassistant.backend.admin.role.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RolePermissionResponse {

    private String role;

    private List<String> permissions;
}
