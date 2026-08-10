package com.enterprise.aiassistant.backend.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// AuthResponse.java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long accessTokenExpiresIn;   // giây
    private long refreshTokenExpiresIn;  // giây
    
}

