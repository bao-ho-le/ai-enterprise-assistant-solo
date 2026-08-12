package com.enterprise.aiassistant.backend.ai.infrastructure.embedding.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbeddingResult {

    private float[] vector;

    private Integer dimension;

    private String model;

    private Integer inputTokens;

}