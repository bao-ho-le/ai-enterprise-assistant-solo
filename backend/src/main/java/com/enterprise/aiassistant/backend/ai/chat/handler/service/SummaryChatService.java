package com.enterprise.aiassistant.backend.ai.chat.handler.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;

public interface SummaryChatService {

    String summarize(AIConversation conversation, String message);
}
