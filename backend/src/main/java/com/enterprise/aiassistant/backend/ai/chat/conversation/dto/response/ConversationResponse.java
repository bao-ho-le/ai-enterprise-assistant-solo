package com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response;


import com.enterprise.aiassistant.backend.ai.chat.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ConversationResponse {

    private Long id;

    private String title;

    private ConversationType conversationType;

    private ConversationStatus status;

    private Long messageCount;

    private LocalDateTime lastMessageAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Null for ACTIVE conversations, set only on soft delete.
    private LocalDateTime deletedAt;
}
