package com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GenerationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// History list item (GET /ai-conversations/{conversationId}/generations): metadata only,
// the content body is fetched separately via GET /generated-contents/{generatedContentId}.
@Getter
@Builder
public class GenerationResponse {

    private Long generationId;

    private GenerationStatus status;

    private LocalDateTime createdAt;

    private Long generatedContentId;
}
