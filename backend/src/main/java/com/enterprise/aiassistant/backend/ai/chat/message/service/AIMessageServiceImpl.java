package com.enterprise.aiassistant.backend.ai.chat.message.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessageDetailResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessagePageResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.ai.chat.message.enums.AIMessageRole;
import com.enterprise.aiassistant.backend.ai.chat.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.chat.message.mapper.AIMessageMapper;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.ai.chat.handler.service.ConversationSummaryChatService;
import com.enterprise.aiassistant.backend.ai.chat.handler.service.GeneralChatService;
import com.enterprise.aiassistant.backend.ai.chat.handler.service.SummaryChatService;
import com.enterprise.aiassistant.backend.ai.chat.intent.enums.Intent;
import com.enterprise.aiassistant.backend.ai.chat.intent.service.IntentClassifier;
import com.enterprise.aiassistant.backend.ai.chat.memory.service.ConversationMemoryService;
import com.enterprise.aiassistant.backend.ai.knowledge.qa.service.DocumentQAService;
import com.enterprise.aiassistant.backend.ai.knowledge.qa.service.QuestionRewriter;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import com.enterprise.aiassistant.backend.document.repository.DocumentChunkRepository;
import com.enterprise.aiassistant.backend.user.entity.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIMessageServiceImpl implements AIMessageService {

    private final AIConversationRepository conversationRepository;

    private final AIConversationHelper aiConversationHelper;

    private final CurrentUserService currentUserService;
    private final AIMessageRepository messageRepository;
    private final AIMessageSourceRepository messageSourceRepository;
    private final DocumentChunkRepository documentChunkRepository;

    private final AIMessageMapper messageMapper;
    private final AIMessageHelper messageHelper;

    private final DocumentQAService documentQAService;
    private final QuestionRewriter questionRewriter;
    private final SummaryChatService summaryChatService;
    private final GeneralChatService generalChatService;
    private final ConversationSummaryChatService conversationSummaryChatService;
    private final IntentClassifier intentClassifier;
    private final ConversationMemoryService conversationMemoryService;


    @Override
    @Transactional
    public MessageResponse sendMessage(
            Long conversationId,
            SendMessageRequest request
    ) {

        messageHelper.validateRequest(request);

        AIConversation conversation = getConversationOrThrow(conversationId);
        messageHelper.validateChatConversationType(conversation.getConversationType());

        AIMessage userMessage = messageMapper.toMessage(conversation, AIMessageRole.USER, request.getContent());
        AIMessage savedUserMessage = messageRepository.save(userMessage);

        AIMessage assistantMessage = answerByIntent(conversation, request.getContent());

        // Chỉ cập nhật conversation memory sau khi assistant trả lời thành công.
        // Tránh lưu user turn với một assistant response giả/null khi LLM hoặc các bước xử lý trước đó thất bại.
        // Memory chỉ phản ánh những lượt hội thoại thực sự đã hoàn tất.
        // Nếu appendTurn() thất bại, transaction sẽ xử lý lỗi theo cơ chế hiện tại.
        if (assistantMessage != null) {
            conversationMemoryService.appendTurn(
                    conversation,
                    request.getContent(),
                    assistantMessage.getContent()
            );
        }

        return messageMapper.toMessageResponse(savedUserMessage, assistantMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public MessagePageResponse getMessages(
            Long conversationId,
            Long beforeId,
            int size
    ) {

        getConversationOrThrow(conversationId);

        // Phần này không dùng Pageble thuần được vì có khả năng lỗi khi lấy message, sử dụng beforeId để giải quyết
        // Do nếu dùng page, để lấy thông tin trang 2, thì khi có message mới, trang 2 đã bị thay đổi, còn dùng
        // beforeId, đóng vai trò như một mark, dù có message mới thì mốc ban đầu không đổi
        // Phần PageRequest (là implements của Pageable) dùng để lấy limit = size
        Slice<AIMessage> page = messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(
                conversationId,
                beforeId != null ? beforeId : Long.MAX_VALUE,
                PageRequest.of(0, size)
        );

        return messageMapper.toMessagePageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public MessageDetailResponse getMessageEvidence(
            Long conversationId,
            Long messageId
    ) {

        getConversationOrThrow(conversationId);

        AIMessage message = getMessageOrThrow(conversationId, messageId);

        List<AIMessageSource> sources =
                messageSourceRepository.findByAiMessageIdOrderByIdAsc(messageId);

        Map<Long, DocumentChunk> chunksById = loadEvidenceChunks(sources);

        return messageMapper.toDetailResponse(message, sources, chunksById);
    }


    // Helper

    // Chỉ DOCUMENT_QA cần retrieval; SUMMARY dùng full document text, GENERAL_CHAT không
    // chạm tới tài liệu, CONVERSATION_SUMMARY chỉ dùng ConversationMemory.
    // DocumentQAService tự lưu assistant message kèm evidence sources.
    private AIMessage answerByIntent(AIConversation conversation, String content) {

        Intent intent = intentClassifier.classify(content);
        requireIntentPermission(intent);

        return switch (intent) {
            // Rewrite câu hỏi (dùng ConversationMemory để resolve reference như "nó", "loại thứ
            // hai") trước khi retrieval, để embedding/Qdrant nhận standalone question thay vì
            // câu phụ thuộc ngữ cảnh. AIMessage user đã lưu content gốc trước đó, không đổi.
            case DOCUMENT_QA ->
                    documentQAService.answer(conversation, questionRewriter.rewrite(conversation, content));
            case SUMMARY -> saveAssistantMessage(conversation, summaryChatService.summarize(conversation, content));
            case GENERAL_CHAT -> saveAssistantMessage(conversation, generalChatService.answer(content));
            case CONVERSATION_SUMMARY ->
                    saveAssistantMessage(conversation, conversationSummaryChatService.summarize(conversation, content));
        };
    }

    // Permission theo đúng khả năng AI được dùng cho tin nhắn này — conversation chỉ có 1
    // type (DOCUMENT_QA) cho mọi chat turn, intent mới là thứ quyết định nhánh nào thực sự chạy.
    // CONVERSATION_SUMMARY chưa có permission tương ứng trong enum nên bỏ ngỏ, không tự suy ra.
    private void requireIntentPermission(Intent intent) {

        switch (intent) {
            case DOCUMENT_QA -> currentUserService.requirePermission(Permission.AI_DOCUMENT_QA);
            case SUMMARY -> currentUserService.requirePermission(Permission.AI_DOCUMENT_SUMMARY);
            case GENERAL_CHAT, CONVERSATION_SUMMARY -> currentUserService.requirePermission(Permission.AI_CHAT);
        }
    }

    private AIMessage saveAssistantMessage(AIConversation conversation, String content) {

        return messageRepository.save(
                messageMapper.toMessage(conversation, AIMessageRole.ASSISTANT, content)
        );
    }

    private Map<Long, DocumentChunk> loadEvidenceChunks(List<AIMessageSource> sources) {

        List<Long> chunkIds = sources.stream().map(AIMessageSource::getChunkId).distinct().toList();

        if (chunkIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return documentChunkRepository.findAllById(chunkIds).stream()
                .collect(Collectors.toMap(DocumentChunk::getId, Function.identity()));
    }

    private AIConversation getConversationOrThrow(Long conversationId) {

        messageHelper.validateConversationId(conversationId);

        AIConversation conversation = conversationRepository
                .findByIdAndStatus(conversationId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> new ConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        aiConversationHelper.validateOwnership(conversation, currentUserService.getCurrentUserId());

        return conversation;
    }

    private AIMessage getMessageOrThrow(Long conversationId, Long messageId) {

        messageHelper.validateMessageId(messageId);

        return messageRepository.findByIdAndConversationId(messageId, conversationId)
                .orElseThrow(() -> new ConversationException(ErrorCode.MESSAGE_NOT_FOUND));
    }


}
