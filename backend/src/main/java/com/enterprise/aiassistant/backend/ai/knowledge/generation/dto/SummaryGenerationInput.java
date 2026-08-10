package com.enterprise.aiassistant.backend.ai.knowledge.generation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SummaryGenerationInput {

    // PARAGRAPH | BULLET_POINTS | STRUCTURED — free text, no enum constraint so
    // the prompt (PromptBuilderService) can be extended without a migration.
    @NotBlank
    private String style;

    @Size(max = 2000)
    private String instructions;

    // Short | Medium | Long
    private String length;

    @Size(max = 200)
    private String audience;

    private String language;

    private Boolean includeActionItems;
}
