package com.enterprise.aiassistant.backend.ai.chat.message.service;

import com.enterprise.aiassistant.backend.ai.chat.message.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessageDetailResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessagePageResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessageResponse;


public interface AIMessageService {

    MessageResponse sendMessage(
            Long conversationId,
            SendMessageRequest request
    );

    // beforeId null -> the latest `size` messages; otherwise the `size` messages
    // immediately preceding that message id. Always returned oldest-first.
    MessagePageResponse getMessages(
            Long conversationId,
            Long beforeId,
            int size
    );

    MessageDetailResponse getMessageEvidence(
            Long conversationId,
            Long messageId
    );
}
