package com.enterprise.aiassistant.backend.auth.service;

import com.enterprise.aiassistant.backend.auth.dto.request.LoginRequest;
import com.enterprise.aiassistant.backend.auth.dto.request.LogoutRequest;
import com.enterprise.aiassistant.backend.auth.dto.request.RefreshTokenRequest;
import com.enterprise.aiassistant.backend.auth.dto.request.RegisterRequest;
import com.enterprise.aiassistant.backend.auth.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request);
}