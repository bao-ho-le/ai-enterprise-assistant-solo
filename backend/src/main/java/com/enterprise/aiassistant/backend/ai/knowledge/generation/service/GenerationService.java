package com.enterprise.aiassistant.backend.ai.knowledge.generation.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.GenerationConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.request.TriggerGenerationRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.TriggerGenerationResponse;

public interface GenerationService {

    TriggerGenerationResponse generate(Long conversationId, TriggerGenerationRequest request);

    GenerationConversationDetailResponse getGenerationDetail(Long generationId);
}
