package com.enterprise.aiassistant.backend.ai.generation.entity;

import com.enterprise.aiassistant.backend.ai.generation.enums.GeneratedDocumentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "generated_content",
        indexes = {
                @Index(
                        name = "idx_generated_content_type",
                        columnList = "generated_type"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GeneratedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Inverse side only - Generation owns the FK. GeneratedContent is no longer a direct
    // child of AIConversation (item 4); its conversation is reached via generation.aiConversation.
    @OneToOne(mappedBy = "generatedContent", fetch = FetchType.LAZY)
    private Generation generation;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_type", nullable = false, length = 50)
    private GeneratedDocumentType generatedType;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
