package com.enterprise.aiassistant.backend.user.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;

    // Frontend dùng để ẩn/hiện UI. Backend vẫn enforce lại ở service layer.
    private Set<String> permissions;

    private Long departmentId;
    private String departmentName;

    private boolean enabled;
    private LocalDateTime createdAt;
}
