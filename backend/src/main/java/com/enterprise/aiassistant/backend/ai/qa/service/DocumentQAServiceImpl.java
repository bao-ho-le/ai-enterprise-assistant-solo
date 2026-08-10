package com.enterprise.aiassistant.backend.ai.qa.service;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.embedding.dto.EmbeddingResult;
import com.enterprise.aiassistant.backend.ai.embedding.service.EmbeddingService;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.llm.service.LLMService;
import com.enterprise.aiassistant.backend.ai.memory.service.ConversationMemoryService;
import com.enterprise.aiassistant.backend.ai.message.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.message.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.ai.message.enums.AIMessageRole;
import com.enterprise.aiassistant.backend.ai.message.mapper.AIMessageMapper;
import com.enterprise.aiassistant.backend.ai.message.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.message.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.ai.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.ai.qa.mapper.QAMapper;
import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.usage.service.AIUsageLogService;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.vectorstore.service.VectorStoreService;
import com.enterprise.aiassistant.backend.document.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentQAServiceImpl implements DocumentQAService {

    private static final int CHAT_TOP_K = 5;

    private final AIConversationDocumentRepository conversationDocumentRepository;
    private final AIMessageRepository messageRepository;
    private final AIMessageSourceRepository messageSourceRepository;
    private final DocumentChunkRepository documentChunkRepository;

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final ConversationMemoryService conversationMemoryService;
    private final PromptBuilderService promptBuilderService;
    private final LLMService llmService;
    private final AIUsageLogService aiUsageLogService;

    private final AIMessageMapper messageMapper;
    private final QAMapper qaMapper;
    private final AIConversationHelper aiConversationHelper;

    @Override
    public AIMessage answer(AIConversation conversation, String question) {

        List<AIConversationDocument> attachedDocuments =
                conversationDocumentRepository.findByAiConversationIdWithDocument(conversation.getId());

        // Không build context / không gọi LLM nếu có tài liệu đính kèm đã bị soft-delete
        aiConversationHelper.validateAttachedDocumentsNotDeleted(attachedDocuments);

        List<Long> attachedVersionIds = attachedDocuments.stream()
                .map(link -> link.getDocumentVersion().getId())
                .toList();

        String model = llmService.getModelName();
        Integer inputTokens = null;
        Integer outputTokens = null;
        Long messageId = null;

        try {
            // Context các lượt trước (chưa gồm câu hỏi hiện tại): giúp LLM hiểu tham chiếu
            String conversationMemory = conversationMemoryService.buildMemoryContext(conversation.getId());

            List<SearchResult> relevantHits = attachedVersionIds.isEmpty()
                    ? List.of()
                    : retrieveRelevantChunks(question, attachedVersionIds);

            String prompt = promptBuilderService.buildDocumentQaPrompt(
                    question,
                    relevantHits.stream().map(hit -> hit.getPayload().getContent()).toList(),
                    conversationMemory
            );

            LLMResponse llmResponse = llmService.generate(
                    qaMapper.toLLMRequest(
                            prompt,
                            conversation.getConversationType(),
                            relevantHits.size()
                    )
            );

            model = llmResponse.getModelName();
            if (llmResponse.getTokenUsage() != null) {
                inputTokens = llmResponse.getTokenUsage().getInputTokens();
                outputTokens = llmResponse.getTokenUsage().getOutputTokens();
            }

            AIMessage assistantMessage = messageRepository.save(
                    messageMapper.toMessage(conversation, AIMessageRole.ASSISTANT, llmResponse.getContent())
            );

            if (!relevantHits.isEmpty()) {
                messageSourceRepository.saveAll(
                        relevantHits.stream().map(hit -> toMessageSource(assistantMessage, hit)).toList()
                );
            }

            messageId = assistantMessage.getId();
            aiUsageLogService.logAiUsage(
                    AIUsageLogRequest.builder()
                            .conversationId(conversation.getId())
                            .messageId(messageId)
                            .conversationType(conversation.getConversationType())
                            .model(model)
                            .inputTokens(inputTokens)
                            .outputTokens(outputTokens)
                            .status(AIUsageStatus.SUCCESS)
                            .build()
            );

            return assistantMessage;

        } catch (RuntimeException ex) {
            aiUsageLogService.logAiUsage(
                    AIUsageLogRequest.builder()
                            .conversationId(conversation.getId())
                            .messageId(messageId)
                            .conversationType(conversation.getConversationType())
                            .model(model)
                            .inputTokens(inputTokens)
                            .outputTokens(outputTokens)
                            .status(AIUsageStatus.FAILED)
                            .build()
            );
            return null;
        }
    }


    // Helper

    // Chỉ lấy tối đa 5 chunks có độ liên quan cao nhất (CHAT_TOP_K)
    private List<SearchResult> retrieveRelevantChunks(String question, List<Long> attachedVersionIds) {

        EmbeddingResult queryEmbedding = embeddingService.embed(question);

        return vectorStoreService.search(queryEmbedding.getVector(), CHAT_TOP_K, null).stream()
                .filter(hit -> attachedVersionIds.contains(hit.getPayload().getDocumentVersionId()))
                .toList();
    }

    private AIMessageSource toMessageSource(AIMessage assistantMessage, SearchResult hit) {

        VectorPayload payload = hit.getPayload();

        return AIMessageSource.builder()
                .aiMessage(assistantMessage)
                .documentChunk(documentChunkRepository.getReferenceById(payload.getChunkId()))
                .similarityScore(hit.getScore())
                .documentVersionId(payload.getDocumentVersionId())
                .chunkId(payload.getChunkId())
                .score(hit.getScore())
                .build();
    }

}
