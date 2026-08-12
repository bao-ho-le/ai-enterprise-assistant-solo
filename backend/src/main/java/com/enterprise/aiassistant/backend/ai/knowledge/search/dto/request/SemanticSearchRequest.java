package com.enterprise.aiassistant.backend.ai.knowledge.search.dto.request;

import lombok.Data;

@Data
public class SemanticSearchRequest {

    private String keyword;

    private Integer topK;

    private Long documentId;

}
