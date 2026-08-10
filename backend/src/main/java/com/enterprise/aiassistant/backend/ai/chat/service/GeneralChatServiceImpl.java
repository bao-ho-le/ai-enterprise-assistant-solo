package com.enterprise.aiassistant.backend.ai.chat.service;

import com.enterprise.aiassistant.backend.ai.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.llm.service.LLMService;
import com.enterprise.aiassistant.backend.ai.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.prompt.service.PromptBuilderService;
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
