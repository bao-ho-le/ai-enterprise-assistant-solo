package com.enterprise.aiassistant.backend.auth.service;

import com.enterprise.aiassistant.backend.auth.config.JwtProperties;
import com.enterprise.aiassistant.backend.auth.entity.RefreshToken;
import com.enterprise.aiassistant.backend.auth.repository.RefreshTokenRepository;
import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthenticationException;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public String createRefreshToken(UserPrincipal userPrincipal) {

        String token = jwtService.generateRefreshToken(userPrincipal);
        String jti = jwtService.extractJti(token);
        String hash = hashToken(token);

        User user = userRepository.findByUsername(userPrincipal.getUsername())
                .orElseThrow(() -> new AuthenticationException(ErrorCode.USER_NOT_FOUND) {});

        RefreshToken refreshToken = RefreshToken.builder()
                .jti(jti)
                .tokenHash(hash)
                .user(user)
                .expiresAt(Instant.now().plusMillis(
                        jwtProperties.getRefreshToken().getExpiration()
                ))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateAndGet(String token) {

        if (!jwtService.isRefreshTokenValid(token)) {
            throw new AuthenticationException(ErrorCode.JWT_INVALID) {};
        }

        String jti = jwtService.extractJti(token);

        RefreshToken storedToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new AuthenticationException(ErrorCode.REFRESH_TOKEN_NOT_FOUND) {});

        // So khớp hash — đảm bảo đúng chính token này, không phải token khác trùng jti
        String incomingHash = hashToken(token);
        if (!MessageDigest.isEqual(
                incomingHash.getBytes(StandardCharsets.UTF_8),
                storedToken.getTokenHash().getBytes(StandardCharsets.UTF_8)
        )) {
            throw new AuthenticationException(ErrorCode.JWT_INVALID) {};
        }

        if (storedToken.isRevoked()) {
            revokeAllByUser(storedToken.getUser());
            throw new AuthenticationException(ErrorCode.REFRESH_TOKEN_REVOKED) {};
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthenticationException(ErrorCode.REFRESH_TOKEN_EXPIRED) {};
        }

        return storedToken;
    }

    @Override
    @Transactional
    public void revokeToken(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void revokeAllByUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String rawToken) {

        if (!jwtService.isRefreshTokenValid(rawToken)) {
            return Optional.empty();
        }

        String jti = jwtService.extractJti(rawToken);
        String incomingHash = hashToken(rawToken);

        return refreshTokenRepository.findByJti(jti)
                .filter(stored -> MessageDigest.isEqual(
                        incomingHash.getBytes(StandardCharsets.UTF_8),
                        stored.getTokenHash().getBytes(StandardCharsets.UTF_8)
                ));
    }

    @Override
    public void verifyExpiration(RefreshToken token) {

        if (token.isRevoked()) {
            revokeAllByUser(token.getUser());
            throw new AuthenticationException(ErrorCode.REFRESH_TOKEN_REVOKED) {};
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthenticationException(ErrorCode.REFRESH_TOKEN_EXPIRED) {};
        }
    }

}