package com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class StartDocumentQaConversationRequest {

    @NotEmpty(message = "documentVersionIds is required")
    private List<Long> documentVersionIds;

    @NotBlank(message = "Message content is required")
    private String content;
}
