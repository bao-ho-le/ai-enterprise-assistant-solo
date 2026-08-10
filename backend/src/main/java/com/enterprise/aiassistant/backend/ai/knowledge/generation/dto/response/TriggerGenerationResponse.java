package com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GenerationStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TriggerGenerationResponse {

    private Long generationId;

    private GenerationStatus status;

    private Long generatedContentId;

    private String errorMessage;
}
