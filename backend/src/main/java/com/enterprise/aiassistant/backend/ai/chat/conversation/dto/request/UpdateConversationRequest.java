package com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateConversationRequest {

    @NotBlank(message = "Title is required")
    private String title;
}
