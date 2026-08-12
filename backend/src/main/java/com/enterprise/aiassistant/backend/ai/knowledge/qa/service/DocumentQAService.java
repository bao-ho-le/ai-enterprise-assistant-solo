package com.enterprise.aiassistant.backend.ai.knowledge.qa.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessage;

public interface DocumentQAService {

    AIMessage answer(AIConversation conversation, String question);
}
