package com.enterprise.aiassistant.backend.ai.chat.handler.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;

public interface ConversationSummaryChatService {

    // Tóm tắt chính cuộc hội thoại (conversation memory) để trả lời user — khác
    // SummaryChatService là tóm tắt tài liệu đính kèm.
    String summarize(
            AIConversation conversation,
            String message
    );
}
