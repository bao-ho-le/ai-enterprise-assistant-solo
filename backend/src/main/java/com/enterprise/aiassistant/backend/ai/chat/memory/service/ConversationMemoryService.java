package com.enterprise.aiassistant.backend.ai.chat.memory.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;

public interface ConversationMemoryService {

    // Context đưa vào prompt của các lượt sau; conversation chưa có memory vẫn trả về
    // chuỗi hợp lệ để prompt không vỡ.
    String buildMemoryContext(Long conversationId);

    void appendTurn(
            AIConversation conversation,
            String userMessage,
            String assistantResponse
    );
}
