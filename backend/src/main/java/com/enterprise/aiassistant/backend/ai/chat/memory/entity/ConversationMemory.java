package com.enterprise.aiassistant.backend.ai.chat.memory.entity;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "conversation_memories",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "ai_conversation_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_conversation_id", nullable = false)
    private AIConversation conversation;

    // Phần hội thoại cũ đã được LLM nén lại; pendingContext là các lượt mới chưa nén.
    @Column(name = "summarized_context", columnDefinition = "TEXT")
    @Builder.Default
    private String summarizedContext = "";

    @Column(name = "pending_context", columnDefinition = "TEXT")
    @Builder.Default
    private String pendingContext = "";

    @Column(name = "context_character_count", nullable = false)
    @Builder.Default
    private Integer contextCharacterCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
