package com.enterprise.aiassistant.backend.ai.chat.service;

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

    private static final String DEFAULT_STYLE = "PARAGRAPH";
    private static final String DEFAULT_LENGTH = "Medium";
    private static final String DEFAULT_AUDIENCE = "General audience";

    // Chat không có form chọn ngôn ngữ như Summary Generation, nên bám theo ngôn ngữ
    // người dùng đang chat, giống cách DOCUMENT_QA prompt đang làm.
    private static final String DEFAULT_LANGUAGE = "the same language as the user's message";

    private final DocumentContextService documentContextService;
    private final PromptBuilderService promptBuilderService;
    private final LLMService llmService;

    private final GenerationHelper generationHelper;
    private final AIConversationHelper aiConversationHelper;
    private final AIMessageHelper messageHelper;

    @Override
    @Transactional(readOnly = true)
    public String summarize(AIConversation conversation, String message) {

        validateSummarizeRequest(conversation, message);

        String documentContext = documentContextService.buildContext(conversation.getId());
        generationHelper.validateSourceDocumentsRequired(documentContext);

        LLMResponse llmResponse = llmService.generate(
                LLMRequest.builder()
                        .prompt(promptBuilderService.buildSummaryPrompt(toSummaryInput(message), documentContext))
                        .conversationType(conversation.getConversationType())
                        .build()
        );

        return llmResponse.getContent();
    }


    // Helper

    private void validateSummarizeRequest(AIConversation conversation, String message) {

        if (conversation == null) {
            throw new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        aiConversationHelper.validateConversationId(conversation.getId());
        messageHelper.validateContent(message);
    }

    // Chat không có form Summary nên dùng default; câu hỏi của user đi vào instructions
    // để prompt hiện tại vẫn phản ánh được yêu cầu cụ thể mà không cần parse gì thêm.
    private SummaryGenerationInput toSummaryInput(String message) {

        SummaryGenerationInput input = new SummaryGenerationInput();
        input.setStyle(DEFAULT_STYLE);
        input.setLength(DEFAULT_LENGTH);
        input.setAudience(DEFAULT_AUDIENCE);
        input.setLanguage(DEFAULT_LANGUAGE);
        input.setInstructions(message);
        input.setIncludeActionItems(false);

        return input;
    }
}
