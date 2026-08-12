package com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedDocumentType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GeneratedContentResponse {

    private Long id;

    private Long aiConversationId;

    private GeneratedDocumentType generatedType;

    private String title;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}