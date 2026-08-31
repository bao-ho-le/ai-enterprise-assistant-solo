package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@RequiredArgsConstructor
public class QdrantConfig {

    private final QdrantProperties properties;

    @Bean
    public QdrantClient qdrantClient() {

        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                properties.getHost(),
                properties.getGrpcPort(),
                properties.getUseTls()
        );

        if (StringUtils.hasText(properties.getApiKey())) {
            builder.withApiKey(properties.getApiKey());
        }

        return new QdrantClient(builder.build());
    }

}