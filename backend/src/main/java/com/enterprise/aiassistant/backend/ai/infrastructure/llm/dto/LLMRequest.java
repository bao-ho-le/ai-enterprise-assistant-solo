package com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LLMRequest {

    private String prompt;

    // Drives the shape of the fake response (email vs report vs QA answer, etc).
    private ConversationType conversationType;

    // DOCUMENT_QA only: how many source chunks were actually retrieved, so the fake
    // answer can cite [1]..[n] against real ai_message_sources rows instead of a
    // fabricated count. Null/unused for generation types.
    private Integer contextItemCount;
}
