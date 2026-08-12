package com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.TriggerGenerationResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StartGenerationConversationResponse {

    private ConversationResponse conversation;

    private TriggerGenerationResponse generation;
}
