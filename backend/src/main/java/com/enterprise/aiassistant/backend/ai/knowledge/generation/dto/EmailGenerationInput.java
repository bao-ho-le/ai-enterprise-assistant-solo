package com.enterprise.aiassistant.backend.ai.knowledge.generation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmailGenerationInput {

    @Size(max = 200)
    private String recipient;

    @NotBlank
    @Size(max = 2000)
    private String purpose;

    // Extra background beyond purpose (prior thread context, requests already made, etc).
    @Size(max = 2000)
    private String optionalContext;

    private String tone;

    // Short | Medium | Long
    private String length;

    private String language;

    private String audience;

    @Size(max = 200)
    private String sender;
}
