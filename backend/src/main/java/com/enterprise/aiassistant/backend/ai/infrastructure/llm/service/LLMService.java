package com.enterprise.aiassistant.backend.ai.infrastructure.llm.service;

import com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto.LLMResponse;

// Single seam between the generation/QA orchestration layer and whichever model
// actually runs. Only FakeLLMService implements this today; swapping in Gemini/OpenAI
// later means adding one new @Service, not touching callers.
public interface LLMService {

    LLMResponse generate(LLMRequest request);

    // Configured model name, available even when generate() hasn't been called yet
    // (e.g. to attribute a failed-before-generate usage log to a model).
    String getModelName();
}
