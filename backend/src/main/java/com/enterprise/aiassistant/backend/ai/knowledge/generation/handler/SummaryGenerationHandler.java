package com.enterprise.aiassistant.backend.ai.knowledge.generation.handler;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.GenerationContext;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.SummaryGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedType;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.helper.GenerationHelper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.service.DocumentContextService;
import com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SummaryGenerationHandler implements GenerationHandler {

    private final GenerationHelper generationHelper;
    private final PromptBuilderService promptBuilderService;
    private final DocumentContextService documentContextService;

    @Override
    public boolean supports(ConversationType type) {
        return type == ConversationType.SUMMARY_GENERATION;
    }

    @Override
    public GenerationContext handle(JsonNode inputData, AIConversation conversation) {

        SummaryGenerationInput input = generationHelper.parseInput(inputData, SummaryGenerationInput.class);

        if (input.getStyle() == null || input.getStyle().isBlank()) {
            throw new AIConversationException(ErrorCode.SUMMARY_GENERATION_STYLE_REQUIRED);
        }

        // String ở đây lưu là text của toàn bộ tài liệu
        String documentContext = documentContextService.buildContext(conversation.getId());
        generationHelper.validateSourceDocumentsRequired(documentContext);

        return GenerationContext.builder()
                .prompt(promptBuilderService.buildSummaryPrompt(input, documentContext))
                .title(generationHelper.truncateTitle("Summary - " + input.getStyle(), 120))
                .generatedType(GeneratedType.SUMMARY)
                .build();
    }
}
