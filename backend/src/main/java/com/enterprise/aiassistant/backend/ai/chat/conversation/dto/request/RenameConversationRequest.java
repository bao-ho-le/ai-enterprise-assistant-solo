package com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RenameConversationRequest {

    @NotBlank(message = "Title is required")
    private String title;
}
