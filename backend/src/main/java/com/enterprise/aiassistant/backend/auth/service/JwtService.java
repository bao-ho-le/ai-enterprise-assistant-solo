package com.enterprise.aiassistant.backend.auth.service;

import com.enterprise.aiassistant.backend.auth.config.JwtProperties;
import com.enterprise.aiassistant.backend.auth.enums.TokenType;
import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    private final SecretKey accessSigningKey;
    private final SecretKey refreshSigningKey;


    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        this.accessSigningKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(
                        jwtProperties.getAccessToken().getSecret()
                )
        );

        this.refreshSigningKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(
                        jwtProperties.getRefreshToken().getSecret()
                )
        );
    }


    public String generateAccessToken(UserPrincipal userPrincipal) {

        return buildToken(
                userPrincipal,
                jwtProperties.getAccessToken().getExpiration(),
                accessSigningKey,
                TokenType.ACCESS_TOKEN
        );
    }


    public String generateRefreshToken(UserPrincipal userPrincipal) {

        return buildToken(
                userPrincipal,
                jwtProperties.getRefreshToken().getExpiration(),
                refreshSigningKey,
                TokenType.REFRESH_TOKEN
        );
    }


    private String buildToken(
            UserPrincipal userPrincipal,
            long expiration,
            SecretKey key,
            TokenType tokenType
    ) {

        JwtBuilder builder = Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + expiration
                        )
                )
                .claim(
                        "type",
                        tokenType.name()
                );


        // Access token chứa quyền
        if (tokenType == TokenType.ACCESS_TOKEN) {

            builder.claim(
                    "role",
                    userPrincipal.getRole().name()
            );
        }


        // Refresh token cần quản lý revoke
        if (tokenType == TokenType.REFRESH_TOKEN) {

            builder.id(
                    UUID.randomUUID().toString()
            );
        }


        return builder
                .signWith(key)
                .compact();
    }


    // =========================
    // ACCESS TOKEN
    // =========================


    public String extractAccessUsername(String token) {

        return extractClaim(
                token,
                accessSigningKey,
                Claims::getSubject
        );
    }


    public String extractAccessRole(String token) {

        return extractClaim(
                token,
                accessSigningKey,
                claims ->
                        claims.get(
                                "role",
                                String.class
                        )
        );
    }


    public boolean isAccessTokenValid(
            String token,
            UserDetails userDetails
    ) {

        String username =
                extractAccessUsername(token);


        return username.equals(
                userDetails.getUsername()
        )
                && !isTokenExpired(
                token,
                accessSigningKey
        )
                && isCorrectTokenType(
                token,
                accessSigningKey,
                TokenType.ACCESS_TOKEN
        );
    }



    // =========================
    // REFRESH TOKEN
    // =========================


    public String extractRefreshUsername(String token) {

        return extractClaim(
                token,
                refreshSigningKey,
                Claims::getSubject
        );
    }


    public String extractJti(String token) {

        return extractClaim(
                token,
                refreshSigningKey,
                Claims::getId
        );
    }


    public boolean isRefreshTokenValid(String token) {

        return !isTokenExpired(
                token,
                refreshSigningKey
        )
                && isCorrectTokenType(
                token,
                refreshSigningKey,
                TokenType.REFRESH_TOKEN
        );
    }



    // =========================
    // COMMON
    // =========================


    public Date extractExpiration(
            String token,
            SecretKey key
    ) {

        return extractClaim(
                token,
                key,
                Claims::getExpiration
        );
    }


    public String extractTokenType(
            String token,
            SecretKey key
    ) {

        return extractClaim(
                token,
                key,
                claims ->
                        claims.get(
                                "type",
                                String.class
                        )
        );
    }


    private boolean isCorrectTokenType(
            String token,
            SecretKey key,
            TokenType expectedType
    ) {

        String type =
                extractTokenType(
                        token,
                        key
                );


        return expectedType.name()
                .equals(type);
    }


    private boolean isTokenExpired(
            String token,
            SecretKey key
    ) {

        return extractExpiration(
                token,
                key
        )
                .before(new Date());
    }



    private Claims extractAllClaims(
            String token,
            SecretKey key
    ) {

        try {

            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (JwtException e) {

            throw new AuthenticationException(ErrorCode.JWT_INVALID) {
            };
        }
    }


    private <T> T extractClaim(
            String token,
            SecretKey key,
            Function<Claims, T> resolver
    ) {

        Claims claims =
                extractAllClaims(
                        token,
                        key
                );

        return resolver.apply(claims);
    }
}