package com.enterprise.aiassistant.backend.ai.message.service;

import com.enterprise.aiassistant.backend.ai.chat.service.ConversationSummaryChatService;
import com.enterprise.aiassistant.backend.ai.chat.service.GeneralChatService;
import com.enterprise.aiassistant.backend.ai.chat.service.SummaryChatService;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.intent.enums.Intent;
import com.enterprise.aiassistant.backend.ai.intent.service.IntentClassifier;
import com.enterprise.aiassistant.backend.ai.memory.service.ConversationMemoryService;
import com.enterprise.aiassistant.backend.ai.message.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.ai.message.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.message.enums.AIMessageRole;
import com.enterprise.aiassistant.backend.ai.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.message.mapper.AIMessageMapper;
import com.enterprise.aiassistant.backend.ai.message.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.message.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.ai.qa.service.DocumentQAService;
import com.enterprise.aiassistant.backend.ai.qa.service.QuestionRewriter;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.document.repository.DocumentChunkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Case 29 của task Question Rewriting: DocumentQAService phải thực sự nhận rewritten
// question (không phải original), còn ConversationMemory update vẫn phải dùng original.
class AIMessageServiceImplQuestionRewriteTest {

    private static final Long CONVERSATION_ID = 1L;
    private static final String ORIGINAL_MESSAGE = "Nói rõ về nó.";
    private static final String REWRITTEN_QUESTION =
            "Giải thích chi tiết về testing được đề cập trong tài liệu.";

    private AIConversationRepository conversationRepository;
    private AIMessageRepository messageRepository;
    private DocumentQAService documentQAService;
    private QuestionRewriter questionRewriter;
    private SummaryChatService summaryChatService;
    private GeneralChatService generalChatService;
    private ConversationSummaryChatService conversationSummaryChatService;
    private IntentClassifier intentClassifier;
    private ConversationMemoryService conversationMemoryService;

    private AIMessageServiceImpl aiMessageService;
    private AIConversation conversation;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(AIConversationRepository.class);
        messageRepository = mock(AIMessageRepository.class);
        AIMessageSourceRepository messageSourceRepository = mock(AIMessageSourceRepository.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        documentQAService = mock(DocumentQAService.class);
        questionRewriter = mock(QuestionRewriter.class);
        summaryChatService = mock(SummaryChatService.class);
        generalChatService = mock(GeneralChatService.class);
        conversationSummaryChatService = mock(ConversationSummaryChatService.class);
        intentClassifier = mock(IntentClassifier.class);
        conversationMemoryService = mock(ConversationMemoryService.class);

        aiMessageService = new AIMessageServiceImpl(
                conversationRepository,
                messageRepository,
                messageSourceRepository,
                documentChunkRepository,
                new AIMessageMapper(),
                new AIMessageHelper(),
                documentQAService,
                questionRewriter,
                summaryChatService,
                generalChatService,
                conversationSummaryChatService,
                intentClassifier,
                conversationMemoryService
        );

        conversation = AIConversation.builder()
                .id(CONVERSATION_ID)
                .conversationType(ConversationType.DOCUMENT_QA)
                .status(ConversationStatus.ACTIVE)
                .build();

        when(conversationRepository.findByIdAndStatus(CONVERSATION_ID, ConversationStatus.ACTIVE))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(AIMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(intentClassifier.classify(ORIGINAL_MESSAGE)).thenReturn(Intent.DOCUMENT_QA);
        when(questionRewriter.rewrite(conversation, ORIGINAL_MESSAGE)).thenReturn(REWRITTEN_QUESTION);
    }

    @Test
    void sendMessage_documentQaIntent_passesRewrittenQuestionToDocumentQaService() {
        AIMessage assistantMessage = assistantMessage();
        when(documentQAService.answer(conversation, REWRITTEN_QUESTION)).thenReturn(assistantMessage);

        aiMessageService.sendMessage(CONVERSATION_ID, sendMessageRequest());

        verify(documentQAService).answer(conversation, REWRITTEN_QUESTION);
        verify(documentQAService, never()).answer(conversation, ORIGINAL_MESSAGE);
    }

    @Test
    void sendMessage_documentQaIntent_memoryUpdateUsesOriginalMessageNotRewritten() {
        AIMessage assistantMessage = assistantMessage();
        when(documentQAService.answer(conversation, REWRITTEN_QUESTION)).thenReturn(assistantMessage);

        aiMessageService.sendMessage(CONVERSATION_ID, sendMessageRequest());

        ArgumentCaptor<String> userMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(conversationMemoryService)
                .appendTurn(eq(conversation), userMessageCaptor.capture(), eq(assistantMessage.getContent()));
        assertThat(userMessageCaptor.getValue()).isEqualTo(ORIGINAL_MESSAGE);
    }

    @Test
    void sendMessage_documentQaIntent_userMessageStillSavedWithOriginalContent() {
        AIMessage assistantMessage = assistantMessage();
        when(documentQAService.answer(conversation, REWRITTEN_QUESTION)).thenReturn(assistantMessage);

        aiMessageService.sendMessage(CONVERSATION_ID, sendMessageRequest());

        ArgumentCaptor<AIMessage> savedMessageCaptor = ArgumentCaptor.forClass(AIMessage.class);
        verify(messageRepository).save(savedMessageCaptor.capture());
        assertThat(savedMessageCaptor.getValue().getContent()).isEqualTo(ORIGINAL_MESSAGE);
    }

    // Case 5/6/7 của task: SUMMARY, GENERAL_CHAT, CONVERSATION_SUMMARY không được chạy
    // Question Rewriter — chỉ DOCUMENT_QA mới cần standalone question cho retrieval.
    @Test
    void sendMessage_summaryIntent_doesNotRunQuestionRewriter() {
        when(intentClassifier.classify(ORIGINAL_MESSAGE)).thenReturn(Intent.SUMMARY);
        when(summaryChatService.summarize(conversation, ORIGINAL_MESSAGE)).thenReturn("Tóm tắt tài liệu.");

        aiMessageService.sendMessage(CONVERSATION_ID, sendMessageRequest());

        verifyNoInteractions(questionRewriter);
    }

    @Test
    void sendMessage_generalChatIntent_doesNotRunQuestionRewriter() {
        when(intentClassifier.classify(ORIGINAL_MESSAGE)).thenReturn(Intent.GENERAL_CHAT);
        when(generalChatService.answer(ORIGINAL_MESSAGE)).thenReturn("Xin chào.");

        aiMessageService.sendMessage(CONVERSATION_ID, sendMessageRequest());

        verifyNoInteractions(questionRewriter);
    }

    @Test
    void sendMessage_conversationSummaryIntent_doesNotRunQuestionRewriter() {
        when(intentClassifier.classify(ORIGINAL_MESSAGE)).thenReturn(Intent.CONVERSATION_SUMMARY);
        when(conversationSummaryChatService.summarize(conversation, ORIGINAL_MESSAGE))
                .thenReturn("Nãy giờ đang nói về testing.");

        aiMessageService.sendMessage(CONVERSATION_ID, sendMessageRequest());

        verifyNoInteractions(questionRewriter);
        verifyNoInteractions(documentQAService);
    }

    private AIMessage assistantMessage() {
        return AIMessage.builder()
                .id(2L)
                .conversation(conversation)
                .role(AIMessageRole.ASSISTANT)
                .content("Testing gồm Unit Test, Integration Test, E2E Test, Slice Test.")
                .build();
    }

    private SendMessageRequest sendMessageRequest() {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent(ORIGINAL_MESSAGE);
        return request;
    }
}
