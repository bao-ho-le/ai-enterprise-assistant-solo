package com.enterprise.aiassistant.backend.ai.vectorstore.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class QdrantConfig {

    private final QdrantProperties properties;

    @Bean
    public QdrantClient qdrantClient() {

        QdrantGrpcClient grpcClient = QdrantGrpcClient.newBuilder(
                properties.getHost(),
                properties.getGrpcPort(),
                properties.getUseTls()
        ).build();

        return new QdrantClient(grpcClient);
    }

}