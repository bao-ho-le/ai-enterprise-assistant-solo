package com.enterprise.aiassistant.backend.ai.infrastructure.embedding.service;

import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.dto.EmbeddingResult;

public interface EmbeddingService {

    EmbeddingResult embed(String text);

    // Configured model name, available even when embed() hasn't been called yet
    // (e.g. to attribute a failed-before-embedding usage log to a model).
    String getModelName();

}
