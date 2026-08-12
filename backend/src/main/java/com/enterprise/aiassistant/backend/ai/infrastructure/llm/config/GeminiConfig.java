package com.enterprise.aiassistant.backend.ai.infrastructure.llm.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {


    @Value("${gemini.llm-model.api-key}")
    private String apiKey;


    @Value("${gemini.llm-model.model-name}")
    private String modelName;

    @Bean
    public ChatModel geminiChatModel() {

        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}