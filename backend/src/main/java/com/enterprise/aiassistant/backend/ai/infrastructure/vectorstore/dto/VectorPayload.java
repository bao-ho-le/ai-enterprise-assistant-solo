package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VectorPayload {

    private Long chunkId;

    private Long documentId;

    private Long documentVersionId;

    private Integer chunkIndex;

    private Integer pageNumber;

    private Integer startChar;

    private Integer endChar;

    private Integer tokenCount;

    private String embeddingModel;

    private String content;

}