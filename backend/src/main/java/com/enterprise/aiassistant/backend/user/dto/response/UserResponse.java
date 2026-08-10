package com.enterprise.aiassistant.backend.user.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

// UserResponse.java
@Getter
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private LocalDateTime createdAt;
}