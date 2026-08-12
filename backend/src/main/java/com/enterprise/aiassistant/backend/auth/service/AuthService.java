package com.enterprise.aiassistant.backend.auth.service;

import com.enterprise.aiassistant.backend.auth.dto.request.LoginRequest;
import com.enterprise.aiassistant.backend.auth.dto.request.RegisterRequest;
import com.enterprise.aiassistant.backend.auth.dto.response.AuthResult;

public interface AuthService {
    AuthResult login(LoginRequest request);

    AuthResult register(RegisterRequest request);

    AuthResult refreshToken(String refreshToken);

    void logout(String refreshToken);
}