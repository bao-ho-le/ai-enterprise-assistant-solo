package com.enterprise.aiassistant.backend.auth.service;

import com.enterprise.aiassistant.backend.auth.entity.RefreshToken;
import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.user.entity.User;

import java.util.Optional;

public interface RefreshTokenService {
    String createRefreshToken(UserPrincipal userPrincipal);
    RefreshToken validateAndGet(String token);
    void revokeToken(RefreshToken refreshToken);
    void revokeAllByUser(User user);
    Optional<RefreshToken> findByToken(String rawToken);
    void verifyExpiration(RefreshToken token);
}
