package com.enterprise.aiassistant.backend.ai.generation.handler;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.generation.dto.EmailGenerationInput;
import com.enterprise.aiassistant.backend.ai.generation.dto.GenerationContext;
import com.enterprise.aiassistant.backend.ai.generation.enums.GeneratedType;
import com.enterprise.aiassistant.backend.ai.generation.helper.GenerationHelper;
import com.enterprise.aiassistant.backend.ai.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailGenerationHandler implements GenerationHandler {

    private final GenerationHelper generationHelper;
    private final PromptBuilderService promptBuilderService;

    @Override
    public boolean supports(ConversationType type) {
        return type == ConversationType.EMAIL_GENERATION;
    }

    @Override
    public GenerationContext handle(JsonNode inputData, AIConversation conversation) {

        EmailGenerationInput input = generationHelper.parseInput(inputData, EmailGenerationInput.class);

        if (input.getPurpose() == null || input.getPurpose().isBlank()) {
            throw new AIConversationException(ErrorCode.EMAIL_GENERATION_PURPOSE_REQUIRED);
        }

        return GenerationContext.builder()
                .prompt(promptBuilderService.buildEmailPrompt(input))
                .title(generationHelper.truncateTitle(input.getPurpose(), 120))
                .generatedType(GeneratedType.EMAIL)
                .build();
    }
}
