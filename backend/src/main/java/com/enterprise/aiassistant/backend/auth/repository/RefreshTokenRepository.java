package com.enterprise.aiassistant.backend.auth.repository;

import com.enterprise.aiassistant.backend.auth.entity.RefreshToken;
import com.enterprise.aiassistant.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByJti(String jti);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteAllByExpiresAtBefore(Instant now);

    void deleteAllByUserId(Long userId);

    void deleteAllByUser(User user);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user")
    void revokeAllByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    void deleteAllExpired(@Param("now") Instant now);
}
