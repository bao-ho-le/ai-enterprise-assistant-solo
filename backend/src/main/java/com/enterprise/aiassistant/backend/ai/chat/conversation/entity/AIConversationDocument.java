package com.enterprise.aiassistant.backend.ai.chat.conversation.entity;

import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;


import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_conversation_documents",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_conversation_document_conversation_version",
                columnNames = {"ai_conversation_id", "document_version_id"}
        ),
        indexes = {
                @Index(
                        name = "idx_ai_conversation_document_conversation_id",
                        columnList = "ai_conversation_id"),
                @Index(
                        name = "idx_ai_conversation_document_version_id",
                        columnList = "document_version_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIConversationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_conversation_id", nullable = false)

    private AIConversation conversation;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
