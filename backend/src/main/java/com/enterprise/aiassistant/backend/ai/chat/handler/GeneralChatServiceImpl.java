package com.enterprise.aiassistant.backend.ai.chat.handler;

import com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.service.LLMService;
import com.enterprise.aiassistant.backend.ai.chat.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service.PromptBuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Câu hỏi không cần tới tài liệu đính kèm: gọi thẳng LLM, không embedding/Qdrant/document context.
@Service
@RequiredArgsConstructor
public class GeneralChatServiceImpl implements GeneralChatService {

    private final PromptBuilderService promptBuilderService;
    private final LLMService llmService;
    private final AIMessageHelper messageHelper;

    @Override
    public String answer(String message) {

        messageHelper.validateContent(message);

        LLMResponse llmResponse = llmService.generate(
                LLMRequest.builder()
                        .prompt(promptBuilderService.buildGeneralChatPrompt(message))
                        .build()
        );

        return llmResponse.getContent();
    }
}
