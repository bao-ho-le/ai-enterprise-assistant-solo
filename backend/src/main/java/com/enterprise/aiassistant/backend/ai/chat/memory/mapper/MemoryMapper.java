package com.enterprise.aiassistant.backend.ai.chat.memory.mapper;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.memory.entity.ConversationMemory;
import org.springframework.stereotype.Component;

@Component
public class MemoryMapper {

    public ConversationMemory toEntity(AIConversation conversation) {
        return ConversationMemory.builder()
                .conversation(conversation)
                .build();
    }
}