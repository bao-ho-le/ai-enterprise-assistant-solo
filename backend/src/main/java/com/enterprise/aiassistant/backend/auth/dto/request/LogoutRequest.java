package com.enterprise.aiassistant.backend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@NoArgsConstructor
public class LogoutRequest {
    @NotBlank
    private String refreshToken;
}
