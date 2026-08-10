package com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenUsage {

    private int inputTokens;

    private int outputTokens;
}
