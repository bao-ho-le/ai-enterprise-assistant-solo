package com.enterprise.aiassistant.backend.ai.chat.service;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.llm.service.LLMService;
import com.enterprise.aiassistant.backend.ai.memory.service.ConversationMemoryService;
import com.enterprise.aiassistant.backend.ai.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// User hỏi về chính cuộc hội thoại: chỉ dùng ConversationMemory, không embedding,
// không Qdrant, không DocumentContextService.
@Service
@RequiredArgsConstructor
public class ConversationSummaryChatServiceImpl implements ConversationSummaryChatService {

    private final ConversationMemoryService conversationMemoryService;
    private final PromptBuilderService promptBuilderService;
    private final LLMService llmService;

    private final AIConversationHelper aiConversationHelper;
    private final AIMessageHelper messageHelper;

    @Override
    @Transactional(readOnly = true)
    public String summarize(
            AIConversation conversation,
            String message
    ) {

        validateSummarizeRequest(conversation, message);

        String conversationMemory = conversationMemoryService.buildMemoryContext(conversation.getId());

        LLMResponse llmResponse = llmService.generate(
                LLMRequest.builder()
                        .prompt(promptBuilderService.buildConversationSummaryPrompt(message, conversationMemory))
                        .conversationType(conversation.getConversationType())
                        .build()
        );

        return llmResponse.getContent();
    }


    // Helper

    private void validateSummarizeRequest(
            AIConversation conversation,
            String message
    ) {

        if (conversation == null) {
            throw new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        aiConversationHelper.validateConversationId(conversation.getId());
        messageHelper.validateContent(message);
    }
}
