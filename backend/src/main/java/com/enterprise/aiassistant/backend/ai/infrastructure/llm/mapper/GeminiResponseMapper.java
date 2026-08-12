package com.enterprise.aiassistant.backend.ai.infrastructure.llm.mapper;

import com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto.TokenUsage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.stereotype.Component;

@Component
public class GeminiResponseMapper {

    public LLMResponse mapToLLMResponse(ChatResponse response, String modelName) {
        TokenUsage tokenUsage = null;

        if (response.tokenUsage() != null) {
            tokenUsage = TokenUsage.builder()
                    .inputTokens(response.tokenUsage().inputTokenCount())
                    .outputTokens(response.tokenUsage().outputTokenCount())
                    .build();
        }

        return LLMResponse.builder()
                .content(response.aiMessage().text())
                .modelName(modelName)
                .tokenUsage(tokenUsage)
                .build();
    }
}
