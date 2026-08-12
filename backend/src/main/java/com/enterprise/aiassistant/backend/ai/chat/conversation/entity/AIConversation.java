package com.enterprise.aiassistant.backend.ai.chat.conversation.entity;


import com.enterprise.aiassistant.backend.ai.chat.conversation.enums.ConversationStatus;


import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_conversations",
        indexes = {
                @Index(name = "idx_ai_conversation_type", columnList = "conversation_type"),
                @Index(name = "idx_ai_conversation_status", columnList = "status"),
                @Index(name = "idx_ai_conversation_created_at", columnList = "created_at"),
                @Index(name = "idx_ai_conversation_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    // Chủ sở hữu conversation. Nullable ở tầng schema vì dữ liệu cũ, nhưng conversation
    // mới luôn có user và mọi thao tác đều bị chặn nếu không phải chủ sở hữu.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "conversation_type", length = 50)
    private ConversationType conversationType;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
