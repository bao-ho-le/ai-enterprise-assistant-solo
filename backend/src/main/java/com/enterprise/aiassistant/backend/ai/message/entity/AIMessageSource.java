package com.enterprise.aiassistant.backend.ai.message.entity;


import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(
        name = "ai_message_sources",
        indexes = {

                @Index(name = "idx_ai_message_source_message_id", columnList = "ai_message_id"),
                @Index(name = "idx_ai_message_source_chunk_id", columnList = "document_chunk_id"),
                @Index(
                        name = "idx_ai_message_sources_document_version_id",
                        columnList = "document_version_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIMessageSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_message_id", nullable = false)
    private AIMessage aiMessage;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_chunk_id", nullable = false)
    private DocumentChunk documentChunk;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "similarity_score")
    private Double similarityScore;

    // Document version chứa chunk được retrieve.
    @Column(name = "document_version_id", nullable = false)
    private Long documentVersionId;

    // Chunk được retrieve.
    @Column(name = "chunk_id", nullable = false)
    private Long chunkId;

    @Column(name = "score")
    private Double score;

}
