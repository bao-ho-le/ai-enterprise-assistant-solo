package com.enterprise.aiassistant.backend.auth.service;

import com.enterprise.aiassistant.backend.auth.config.JwtProperties;
import com.enterprise.aiassistant.backend.auth.dto.request.LoginRequest;
import com.enterprise.aiassistant.backend.auth.dto.request.LogoutRequest;
import com.enterprise.aiassistant.backend.auth.dto.request.RefreshTokenRequest;
import com.enterprise.aiassistant.backend.auth.dto.request.RegisterRequest;
import com.enterprise.aiassistant.backend.auth.dto.response.AuthResponse;
import com.enterprise.aiassistant.backend.auth.entity.RefreshToken;
import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthenticationException;
import com.enterprise.aiassistant.backend.user.entity.Role;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {


    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;



    @Override
    public AuthResponse login(LoginRequest request) {


        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUserName(),
                                request.getPassword()
                        )
                );


        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String accessToken = jwtService.generateAccessToken(userPrincipal);
        String refreshToken = refreshTokenService.createRefreshToken(userPrincipal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(3600)  // 1 hour
                .refreshTokenExpiresIn(86400)  // 24 hours
                .build();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUserName())) {
            throw new AuthenticationException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthenticationException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .username(request.getUserName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .enabled(true)
                .role(Role.USER)
                .build();

        userRepository.save(user);

        // Tự động login sau khi đăng ký
        String accessToken = jwtService.generateAccessToken(UserPrincipal.from(user));
        String refreshToken = refreshTokenService.createRefreshToken(UserPrincipal.from(user));

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtProperties.getAccessToken().getExpiration())
                .refreshTokenExpiresIn(jwtProperties.getRefreshToken().getExpiration())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenService
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN));

        refreshTokenService.verifyExpiration(storedToken);

        User user = storedToken.getUser();

        // Revoke token cũ, tạo mới (rotation) — tránh reuse refresh token
        refreshTokenService.revokeToken(storedToken);
        String newRefreshToken = refreshTokenService.createRefreshToken(UserPrincipal.from(user));
        String newAccessToken = jwtService.generateAccessToken(UserPrincipal.from(user));

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtProperties.getAccessToken().getExpiration())
                .refreshTokenExpiresIn(jwtProperties.getRefreshToken().getExpiration())
                .build();
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        RefreshToken storedToken = refreshTokenService
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN));

        refreshTokenService.revokeToken(storedToken);
    }
}