package com.enterprise.aiassistant.backend.auth.entity;

import com.enterprise.aiassistant.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_token_jti", columnList = "jti"),
                @Index(name = "idx_refresh_token_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * JWT jti claim
     */
    @Column(nullable = false, unique = true)
    private String jti;

    /**
     * Hash của refresh token (không lưu token gốc)
     */
    @Column(nullable = false, unique = true, length = 512)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    /**
     * Thời điểm hết hạn
     */
    @Column(nullable = false)
    private Instant expiresAt;

    /**
     * Token đã bị logout/revoke chưa
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /**
     * Thời điểm tạo
     */
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;


}