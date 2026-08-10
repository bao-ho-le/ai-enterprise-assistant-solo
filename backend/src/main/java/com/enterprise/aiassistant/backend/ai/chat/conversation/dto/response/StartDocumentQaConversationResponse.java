package com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response;

import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessageResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StartDocumentQaConversationResponse {

    private ConversationResponse conversation;

    private MessageResponse message;
}
