package com.enterprise.aiassistant.backend.ai.chat.message.helper;

import com.enterprise.aiassistant.backend.ai.chat.message.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class AIMessageHelper {

    // Chat (sendMessage) only makes sense for conversation types with a Q&A turn.
    // Generation types (email/report/...) go through GenerationService's /generate instead.
    private static final Set<ConversationType> CHAT_CONVERSATION_TYPES = EnumSet.of(
            ConversationType.DOCUMENT_QA
            // SEMANTIC_SEARCH: add here once that flow gets a chat turn too.
    );

    public void validateConversationId(Long conversationId) {

        if (conversationId == null) {
            throw new ConversationException(ErrorCode.CONVERSATION_ID_REQUIRED);
        }
    }

    public void validateChatConversationType(ConversationType conversationType) {

        if (!CHAT_CONVERSATION_TYPES.contains(conversationType)) {
            throw new ConversationException(ErrorCode.CONVERSATION_TYPE_NOT_CHAT);
        }
    }

    public void validateRequest(SendMessageRequest request) {

        if (request == null) {
            throw new ConversationException(ErrorCode.REQUEST_REQUIRED);
        }

        validateContent(request.getContent());
    }

    public void validateContent(String content) {

        if (content == null || content.isBlank()) {
            throw new ConversationException(ErrorCode.MESSAGE_CONTENT_REQUIRED);
        }

        if (content.length() > 10000) {
            throw new ConversationException(ErrorCode.MESSAGE_CONTENT_TOO_LONG);
        }
    }

    public void validateMessageId(Long messageId) {

        if (messageId == null) {
            throw new ConversationException(ErrorCode.MESSAGE_ID_REQUIRED);
        }
    }
}
