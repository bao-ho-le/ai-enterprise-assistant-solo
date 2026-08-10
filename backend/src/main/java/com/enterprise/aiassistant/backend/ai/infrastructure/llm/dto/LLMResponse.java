package com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LLMResponse {

    private String content;

    private TokenUsage tokenUsage;

    private String modelName;
}
