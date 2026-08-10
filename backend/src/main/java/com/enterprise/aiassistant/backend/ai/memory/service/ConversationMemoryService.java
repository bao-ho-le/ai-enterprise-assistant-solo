package com.enterprise.aiassistant.backend.ai.memory.service;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;

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
