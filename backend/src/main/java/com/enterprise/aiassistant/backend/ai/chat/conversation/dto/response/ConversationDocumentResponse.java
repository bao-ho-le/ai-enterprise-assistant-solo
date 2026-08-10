package com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationDocumentResponse {

    private Long documentId;

    private Long documentVersionId;

    private String documentTitle;

    private Integer versionNumber;

    private LocalDateTime attachedAt;
}