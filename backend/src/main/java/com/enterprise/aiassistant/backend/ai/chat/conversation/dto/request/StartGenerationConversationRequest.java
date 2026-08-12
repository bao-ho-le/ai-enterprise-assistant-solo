package com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StartGenerationConversationRequest {

    @NotNull(message = "Conversation type is required")
    private ConversationType conversationType;

    // Optional: only report/summary generation actually reads attached documents.
    private List<Long> documentVersionIds;

    @NotNull(message = "Input data is required")
    private Map<String, Object> inputData;
}
