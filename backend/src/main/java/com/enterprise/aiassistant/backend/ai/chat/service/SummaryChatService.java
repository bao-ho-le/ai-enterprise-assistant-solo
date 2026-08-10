package com.enterprise.aiassistant.backend.ai.chat.service;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;

public interface SummaryChatService {

    String summarize(AIConversation conversation, String message);
}
