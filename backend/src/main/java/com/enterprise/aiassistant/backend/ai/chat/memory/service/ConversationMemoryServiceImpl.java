package com.enterprise.aiassistant.backend.ai.chat.memory.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.service.LLMService;
import com.enterprise.aiassistant.backend.ai.chat.memory.entity.ConversationMemory;
import com.enterprise.aiassistant.backend.ai.chat.memory.helper.ConversationMemoryHelper;
import com.enterprise.aiassistant.backend.ai.chat.memory.mapper.MemoryMapper;
import com.enterprise.aiassistant.backend.ai.chat.memory.repository.ConversationMemoryRepository;
import com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service.PromptBuilderService;

import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Memory nén của conversation dành cho AI: summarizedContext (đã nén) + pendingContext
// (các lượt mới). AIMessage vẫn là lịch sử đầy đủ, service này không query message.
@Service
@RequiredArgsConstructor
public class ConversationMemoryServiceImpl implements ConversationMemoryService {

    private final ConversationMemoryRepository conversationMemoryRepository;

    private final PromptBuilderService promptBuilderService;
    private final LLMService llmService;

    private final ConversationMemoryHelper memoryHelper;
    private final AIConversationHelper aiConversationHelper;
    private final ConversationMemoryHelper conversationMemoryHelper;
    private final MemoryMapper memoryMapper;


    @Override
    @Transactional(readOnly = true)
    public String buildMemoryContext(Long conversationId) {

        aiConversationHelper.validateConversationId(conversationId);

        return conversationMemoryRepository.findByConversationId(conversationId)
                .map(memory -> memoryHelper.buildContext(memory.getSummarizedContext(), memory.getPendingContext()))
                .orElseGet(memoryHelper::emptyContext);
    }

    @Override
    @Transactional
    public void appendTurn(
            AIConversation conversation,
            String userMessage,
            String assistantResponse
    ) {

        conversationMemoryHelper.validateTurn(conversation, userMessage, assistantResponse);

        // Lazy-create: chỉ conversation thực sự có lượt chat mới sinh memory row.
        ConversationMemory memory = conversationMemoryRepository
                .findByConversationIdForUpdate(conversation.getId())
                .orElseGet(() -> memoryMapper.toEntity(conversation));

        memory.setPendingContext(
                memoryHelper.appendTurn(memory.getPendingContext(), userMessage, assistantResponse)
        );

        // Đếm lại từ chuỗi thật thay vì cộng dồn count cũ, tránh count bị lệch.
        memory.setContextCharacterCount(
                memoryHelper.calculateCharacterCount(memory.getSummarizedContext(), memory.getPendingContext())
        );

        if (memoryHelper.needsSummarization(memory.getContextCharacterCount())) {
            compress(memory);
        }

        conversationMemoryRepository.save(memory);
    }


    // Helper

    // Nén thất bại thì giữ nguyên summarizedContext + pendingContext: lượt chat đã trả lời
    // thành công, memory chỉ hoãn nén tới lượt sau chứ không được mất dữ liệu.
    private void compress(ConversationMemory memory) {

        try {
            LLMResponse llmResponse = llmService.generate(
                    LLMRequest.builder()
                            .prompt(promptBuilderService.buildConversationMemorySummaryPrompt(
                                    memory.getSummarizedContext(),
                                    memory.getPendingContext()
                            ))
                            .build()
            );

            String generatedSummary = llmResponse.getContent();

            if (generatedSummary == null || generatedSummary.isBlank()) {
                return;
            }

            memory.setSummarizedContext(generatedSummary.trim());
            memory.setPendingContext("");
            memory.setContextCharacterCount(memory.getSummarizedContext().length());

        } catch (LLMException e) {
            // giữ nguyên memory hiện tại
        }
    }


}
