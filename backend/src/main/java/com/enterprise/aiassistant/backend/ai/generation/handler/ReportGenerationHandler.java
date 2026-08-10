package com.enterprise.aiassistant.backend.ai.generation.handler;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.generation.dto.GenerationContext;
import com.enterprise.aiassistant.backend.ai.generation.dto.ReportGenerationInput;
import com.enterprise.aiassistant.backend.ai.generation.enums.GeneratedType;
import com.enterprise.aiassistant.backend.ai.generation.helper.GenerationHelper;
import com.enterprise.aiassistant.backend.ai.generation.service.DocumentContextService;
import com.enterprise.aiassistant.backend.ai.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportGenerationHandler implements GenerationHandler {

    private final GenerationHelper generationHelper;
    private final PromptBuilderService promptBuilderService;
    private final DocumentContextService documentContextService;

    @Override
    public boolean supports(ConversationType type) {
        return type == ConversationType.REPORT_GENERATION;
    }

    @Override
    public GenerationContext handle(JsonNode inputData, AIConversation conversation) {

        ReportGenerationInput input = generationHelper.parseInput(inputData, ReportGenerationInput.class);

        if (input.getTitle() == null || input.getTitle().isBlank()) {
            throw new AIConversationException(ErrorCode.REPORT_GENERATION_TITLE_REQUIRED);
        }

        // String ở đây lưu là text của toàn bộ tài liệu
        String documentContext = documentContextService.buildContext(conversation.getId());
        generationHelper.validateSourceDocumentsRequired(documentContext);

        return GenerationContext.builder()
                .prompt(promptBuilderService.buildReportPrompt(input, documentContext))
                .title(generationHelper.truncateTitle(input.getTitle(), 120))
                .generatedType(GeneratedType.REPORT)
                .build();
    }
}
