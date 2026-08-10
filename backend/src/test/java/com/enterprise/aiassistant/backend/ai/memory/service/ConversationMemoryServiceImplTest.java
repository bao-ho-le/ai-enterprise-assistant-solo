package com.enterprise.aiassistant.backend.ai.memory.service;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.llm.service.LLMService;
import com.enterprise.aiassistant.backend.ai.memory.entity.ConversationMemory;
import com.enterprise.aiassistant.backend.ai.memory.helper.ConversationMemoryHelper;
import com.enterprise.aiassistant.backend.ai.memory.repository.ConversationMemoryRepository;
import com.enterprise.aiassistant.backend.ai.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Kiểm tra logic memory: append giữ cả 2 vai, count = summarized + pending,
// >= 16.000 ký tự thì nén bằng LLM, nén lỗi thì không mất pendingContext.
class ConversationMemoryServiceImplTest {

    private static final Long CONVERSATION_ID = 1L;

    private ConversationMemoryRepository conversationMemoryRepository;
    private LLMService llmService;
    private ConversationMemoryServiceImpl conversationMemoryService;

    private AIConversation conversation;

    @BeforeEach
    void setUp() {
        conversationMemoryRepository = mock(ConversationMemoryRepository.class);
        llmService = mock(LLMService.class);

        conversationMemoryService = new ConversationMemoryServiceImpl(
                conversationMemoryRepository,
                new PromptBuilderService(),
                llmService,
                new ConversationMemoryHelper(),
                new AIConversationHelper(),
                new AIMessageHelper()
        );

        conversation = AIConversation.builder().id(CONVERSATION_ID).build();
    }

    @Test
    void appendTurn_belowLimit_keepsPendingContextAndSkipsSummarization() {
        givenExistingMemory("", "");

        conversationMemoryService.appendTurn(conversation, "Doanh thu 2025?", "10 triệu USD.");

        ConversationMemory saved = captureSavedMemory();
        assertThat(saved.getPendingContext()).contains("USER:", "Doanh thu 2025?", "ASSISTANT:", "10 triệu USD.");
        assertThat(saved.getSummarizedContext()).isEmpty();
        assertThat(saved.getContextCharacterCount())
                .isEqualTo(saved.getSummarizedContext().length() + saved.getPendingContext().length());
        verifyNoInteractions(llmService);
    }

    @Test
    void appendTurn_atLimit_compressesWithLlmAndClearsPendingContext() {
        givenExistingMemory("", "x".repeat(16_000));
        when(llmService.generate(any(LLMRequest.class)))
                .thenReturn(LLMResponse.builder().content("s".repeat(6_000)).build());

        conversationMemoryService.appendTurn(conversation, "Tăng bao nhiêu?", "Tăng 12%.");

        ConversationMemory saved = captureSavedMemory();
        assertThat(saved.getSummarizedContext()).hasSize(6_000);
        assertThat(saved.getPendingContext()).isEmpty();
        assertThat(saved.getContextCharacterCount()).isEqualTo(6_000);
        verify(llmService).generate(any(LLMRequest.class));
    }

    @Test
    void appendTurn_compressionFails_keepsWholeContext() {
        givenExistingMemory("old summary", "x".repeat(16_000));
        when(llmService.generate(any(LLMRequest.class)))
                .thenThrow(new LLMException(ErrorCode.LLM_GENERATION_FAILED));

        conversationMemoryService.appendTurn(conversation, "Tăng bao nhiêu?", "Tăng 12%.");

        ConversationMemory saved = captureSavedMemory();
        assertThat(saved.getSummarizedContext()).isEqualTo("old summary");
        assertThat(saved.getPendingContext()).startsWith("x".repeat(16_000)).contains("Tăng 12%.");
        assertThat(saved.getContextCharacterCount())
                .isEqualTo(saved.getSummarizedContext().length() + saved.getPendingContext().length());
    }

    @Test
    void buildMemoryContext_withoutMemory_returnsPlaceholderInsteadOfNull() {
        when(conversationMemoryRepository.findByConversationId(CONVERSATION_ID)).thenReturn(Optional.empty());

        assertThat(conversationMemoryService.buildMemoryContext(CONVERSATION_ID))
                .isEqualTo("No previous conversation context.");
    }

    private void givenExistingMemory(String summarizedContext, String pendingContext) {
        ConversationMemory memory = ConversationMemory.builder()
                .id(10L)
                .conversation(conversation)
                .summarizedContext(summarizedContext)
                .pendingContext(pendingContext)
                .contextCharacterCount(summarizedContext.length() + pendingContext.length())
                .build();

        when(conversationMemoryRepository.findByConversationIdForUpdate(CONVERSATION_ID))
                .thenReturn(Optional.of(memory));
    }

    private ConversationMemory captureSavedMemory() {
        ArgumentCaptor<ConversationMemory> captor = ArgumentCaptor.forClass(ConversationMemory.class);
        verify(conversationMemoryRepository).save(captor.capture());
        return captor.getValue();
    }
}
