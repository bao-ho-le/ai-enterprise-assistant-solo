package com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper;

import com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import org.springframework.stereotype.Component;

@Component
public class LLMHelper {

    public void validateGenerateRequest(LLMRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new LLMException(ErrorCode.LLM_INVALID_PROMPT);
        }
    }
}
