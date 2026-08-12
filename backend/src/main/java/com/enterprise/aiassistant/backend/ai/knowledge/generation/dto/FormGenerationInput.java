package com.enterprise.aiassistant.backend.ai.knowledge.generation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FormGenerationInput {

    @NotBlank
    @Size(max = 2000)
    private String purpose;

    @Size(max = 2000)
    private String fields;
}
