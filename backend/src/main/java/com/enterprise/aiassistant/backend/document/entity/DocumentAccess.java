package com.enterprise.aiassistant.backend.document.entity;

import com.enterprise.aiassistant.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// Chia sẻ tài liệu cho 1 user cụ thể. Phase này chỉ có quyền READ (kèm DOWNLOAD),
// không kéo theo UPDATE/DELETE/MANAGE_ACCESS.
@Entity
@Table(
        name = "document_accesses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_access_document_user",
                columnNames = {"document_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_document_access_document_id", columnList = "document_id"),
                @Index(name = "idx_document_access_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    private User grantedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
