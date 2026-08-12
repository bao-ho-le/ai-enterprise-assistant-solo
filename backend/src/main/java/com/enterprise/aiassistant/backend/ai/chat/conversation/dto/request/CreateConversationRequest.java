package com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConversationRequest {

    // Optional: blank/omitted -> AIConversationHelper fills in a default based on
    // conversationType + creation time.
    private String title;

    @NotNull(message = "Conversation type is required")
    private ConversationType conversationType;
}
