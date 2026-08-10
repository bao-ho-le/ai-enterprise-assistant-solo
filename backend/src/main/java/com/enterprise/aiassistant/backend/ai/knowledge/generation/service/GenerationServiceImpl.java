package com.enterprise.aiassistant.backend.ai.knowledge.generation.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.ConversationDocumentResponse;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.GenerationConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.conversation.mapper.AIConversationMapper;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.GenerationContext;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.request.TriggerGenerationRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.TriggerGenerationResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.Generation;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedDocumentType;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GenerationStatus;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.handler.GenerationHandler;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.helper.GenerationHelper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.mapper.GeneratedMapper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.mapper.GenerationMapper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GeneratedContentRepository;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GenerationRepository;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.service.LLMService;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.analytics.usage.service.AIUsageLogService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerationServiceImpl implements GenerationService {

    private final AIConversationRepository conversationRepository;
    private final AIConversationDocumentRepository conversationDocumentRepository;
    private final GenerationRepository generationRepository;
    private final GeneratedContentRepository generatedContentRepository;

    private final AIConversationHelper aiConversationHelper;
    private final AIConversationMapper aiConversationMapper;
    private final GenerationHelper generationHelper;
    private final GeneratedMapper generatedMapper;
    private final GenerationMapper generationMapper;
    private final AIUsageLogService aiUsageLogService;
    private final LLMService llmService;

    private final List<GenerationHandler> handlers;

    @Override
    @Transactional
    public TriggerGenerationResponse generate(Long conversationId, TriggerGenerationRequest request) {

        // 1. Validate request và conversation

        aiConversationHelper.validateConversationId(conversationId);
        generationHelper.validateTriggerRequest(request);

        AIConversation conversation = conversationRepository
                .findByIdAndStatus(conversationId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        aiConversationHelper.validateGenerationConversationType(conversation.getConversationType());

        // 2. Resolve handler và chuẩn bị dữ liệu đầu vào

        GenerationHandler handler = resolveHandler(conversation.getConversationType());

        JsonNode inputData = generationMapper.toJsonNode(request.getInputData());

        GenerationContext context = handler.handle(inputData, conversation);

        // 3. Khởi tạo Generation và đánh dấu bắt đầu xử lý

        Generation generation = generationRepository.save(
                generationMapper.toGeneration(conversation, context, inputData)
        );

        generation.setStatus(GenerationStatus.RUNNING);
        generationRepository.save(generation);

        String model = llmService.getModelName();
        Integer inputTokens = null;
        Integer outputTokens = null;

        try {

            // 4. Gọi LLM để sinh nội dung

            LLMResponse llmResponse = llmService.generate(
                    LLMRequest.builder()
                            .prompt(context.getPrompt())
                            .conversationType(conversation.getConversationType())
                            .build()
            );
            model = llmResponse.getModelName();
            if (llmResponse.getTokenUsage() != null) {
                inputTokens = llmResponse.getTokenUsage().getInputTokens();
                outputTokens = llmResponse.getTokenUsage().getOutputTokens();
            }

            // 5. Lưu GeneratedContent và cập nhật Generation thành công

            GeneratedContent generatedContent = generatedContentRepository.save(
                    generatedMapper.toCreateGeneratedContentObject(
                            GeneratedDocumentType.valueOf(context.getGeneratedType().name()),
                            context.getTitle(),
                            llmResponse.getContent()
                    )
            );

            generation.setGeneratedContent(generatedContent);
            generation.setStatus(GenerationStatus.COMPLETED);
            generationRepository.save(generation);

            // 6. Ghi nhận usage thành công và trả kết quả

            logUsage(conversation, model, inputTokens, outputTokens, AIUsageStatus.SUCCESS, null);

            return generationMapper.toTriggerGenerationResponse(generation, generatedContent);

        } catch (RuntimeException ex) {

            // Đánh dấu thất bại, ghi log usage và ném exception

            generation.setStatus(GenerationStatus.FAILED);
            generation.setErrorMessage(ex.getMessage());
            generationRepository.save(generation);

            logUsage(conversation, model, inputTokens, outputTokens, AIUsageStatus.FAILED, ex.getMessage());

            throw new AIConversationException(ErrorCode.GENERATION_RUN_FAILED, ex);
        }
    }

    // Hiện đang không được dùng
    @Override
    @Transactional(readOnly = true)
    public GenerationConversationDetailResponse getGenerationDetail(Long generationId) {

        generationHelper.validateGenerationId(generationId);

        Generation generation = generationRepository.findById(generationId)
                .orElseThrow(() -> new AIConversationException(ErrorCode.GENERATION_NOT_FOUND));

        AIConversation conversation = generation.getAiConversation();

        // Nếu conversation type là email thì không có attach document
        boolean isEmailGeneration = conversation.getConversationType() == ConversationType.EMAIL_GENERATION;
        List<ConversationDocumentResponse> attachedDocuments =
                isEmailGeneration
                        ? null
                        : conversationDocumentRepository.findByAiConversationIdWithDocument(conversation.getId())
                                .stream()
                                .map(aiConversationMapper::toConversationDocumentResponse)
                                .toList();
        boolean hasDeletedAttachedDocuments = !isEmailGeneration
                && conversationDocumentRepository.existsByConversationIdAndDocumentVersionDocumentStatus(
                        conversation.getId(), DocumentStatus.DELETED);

        return aiConversationMapper.toGenerationDetailResponse(
                conversation,
                generation,
                attachedDocuments,
                hasDeletedAttachedDocuments
        );
    }

    // Strategy Pattern: chọn handler theo conversationType, không rẽ nhánh theo type ở đâu khác.
    // Đặt private ở đây (không đưa vào GenerationHelper) để tránh cycle: các handler tự inject
    // GenerationHelper (dùng parseInput/truncateTitle), nên Helper không được quay lại phụ thuộc handlers.
    private GenerationHandler resolveHandler(ConversationType conversationType) {
        return handlers.stream()
                .filter(handler -> handler.supports(conversationType))
                .findFirst()
                .orElseThrow(() -> new AIConversationException(ErrorCode.GENERATION_HANDLER_NOT_FOUND));
    }

    private void logUsage(
            AIConversation conversation,
            String model,
            Integer inputTokens,
            Integer outputTokens,
            AIUsageStatus status,
            String errorMessage
    ) {
        aiUsageLogService.logAiUsage(AIUsageLogRequest.builder()
                .conversationId(conversation.getId())
                .conversationType(conversation.getConversationType())
                .model(model)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .status(status)
                .errorMessage(errorMessage)
                .build());
    }
}
