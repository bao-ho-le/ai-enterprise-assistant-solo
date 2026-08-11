package com.enterprise.aiassistant.backend.department.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentMemberResponse {

    private Long userId;

    private String username;

    private String fullName;

    private String email;

    private String role;

    private boolean enabled;
}
