package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class SearchResult {

    private Long pointId;

    private Double score;

    private VectorPayload payload;

}