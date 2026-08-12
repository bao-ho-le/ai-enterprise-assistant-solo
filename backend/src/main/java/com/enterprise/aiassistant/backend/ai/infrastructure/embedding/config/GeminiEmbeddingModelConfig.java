package com.enterprise.aiassistant.backend.ai.infrastructure.embedding.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GeminiEmbeddingModelConfig {

    private final GeminiProperties properties;

    // Exposed as the provider-agnostic LangChain4j EmbeddingModel type so
    // GeminiEmbeddingService only depends on the SDK abstraction, not this Gemini class.
    @Bean
    public EmbeddingModel embeddingModel() {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getEmbeddingModel())
                .build();
    }

}
