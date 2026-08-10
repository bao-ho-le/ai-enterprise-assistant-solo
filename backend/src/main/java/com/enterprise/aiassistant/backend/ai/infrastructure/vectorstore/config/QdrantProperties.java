package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "qdrant")
@Getter
@Setter
public class QdrantProperties {

    private String host;

    private Integer grpcPort;

    private Integer restPort;

    private String collectionName;

    private Integer vectorSize;

    // Ngưỡng cosine similarity tối thiểu để 1 kết quả search được coi là liên quan.
    private Float scoreThreshold;

    private Boolean useTls;

    private String apiKey;
}