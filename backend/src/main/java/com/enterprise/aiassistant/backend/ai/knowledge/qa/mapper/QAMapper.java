package com.enterprise.aiassistant.backend.ai.knowledge.qa.mapper;


import com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import org.springframework.stereotype.Component;

@Component
public class QAMapper {

    public LLMRequest toLLMRequest(
            String prompt,
            ConversationType conversationType,
            int contextItemCount
    ) {
        return LLMRequest.builder()
                .prompt(prompt)
                .conversationType(conversationType)
                .contextItemCount(contextItemCount)
                .build();
    }
}
