package com.enterprise.aiassistant.backend.ai.knowledge.generation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportGenerationInput {

    @NotBlank
    @Size(max = 500)
    private String title;

    @Size(max = 2000)
    private String instructions;

    @Size(max = 200)
    private String audience;

    // Short | Medium | Long
    private String length;

    // ISO date strings (yyyy-MM-dd), free text — no reporting-period math done on these,
    // they only ever flow into the prompt as context for the fake/real model.
    private String fromDate;

    private String toDate;

    private String language;
}
