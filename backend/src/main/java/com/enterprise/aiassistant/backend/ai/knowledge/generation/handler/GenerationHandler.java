package com.enterprise.aiassistant.backend.ai.knowledge.generation.handler;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.GenerationContext;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.fasterxml.jackson.databind.JsonNode;

// Strategy per generation ConversationType. GenerationService picks one via supports()
// and never branches on type itself — adding a new generation type is one new @Component.
public interface GenerationHandler {

    boolean supports(ConversationType type);

    GenerationContext handle(JsonNode inputData, AIConversation conversation);
}
