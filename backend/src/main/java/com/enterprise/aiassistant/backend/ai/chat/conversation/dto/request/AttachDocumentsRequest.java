package com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AttachDocumentsRequest {

    @NotEmpty(message = "documentVersionIds is required")
    private List<Long> documentVersionIds;
}
