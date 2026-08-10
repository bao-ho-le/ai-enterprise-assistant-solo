package com.enterprise.aiassistant.backend.ai.chat.service;

import com.enterprise.aiassistant.backend.ai.chat.helper.ChatHelper;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.generation.dto.SummaryGenerationInput;
import com.enterprise.aiassistant.backend.ai.generation.helper.GenerationHelper;
import com.enterprise.aiassistant.backend.ai.generation.service.DocumentContextService;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.llm.service.LLMService;
import com.enterprise.aiassistant.backend.ai.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Yêu cầu tóm tắt gõ tự do trong chat: dùng lại nguyên DocumentContextService (full document
// text) và buildSummaryPrompt của Summary Generation, không retrieval, không đổi conversation type.
@Service
@RequiredArgsConstructor
public class SummaryChatServiceImpl implements SummaryChatService {

    private final DocumentContextService documentContextService;
    private final PromptBuilderService promptBuilderService;
    private final LLMService llmService;

    private final GenerationHelper generationHelper;
    private final AIConversationHelper aiConversationHelper;
    private final AIMessageHelper messageHelper;
    private final ChatHelper chatHelper;

    @Override
    @Transactional(readOnly = true)
    public String summarize(AIConversation conversation, String message) {

        aiConversationHelper.validateConversationId(conversation.getId());
        messageHelper.validateContent(message);

        String documentContext = documentContextService.buildContext(conversation.getId());
        generationHelper.validateSourceDocumentsRequired(documentContext);

        LLMResponse llmResponse = llmService.generate(
                LLMRequest.builder()
                        .prompt(promptBuilderService.buildSummaryPrompt(chatHelper.toSummaryInput(message), documentContext))
                        .conversationType(conversation.getConversationType())
                        .build()
        );

        return llmResponse.getContent();
    }

}
