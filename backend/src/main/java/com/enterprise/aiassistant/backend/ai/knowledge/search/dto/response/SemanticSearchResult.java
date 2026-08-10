package com.enterprise.aiassistant.backend.ai.knowledge.search.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchResult {

    private Long documentId;

    private Long versionId;

    private Long chunkId;

    private Double score;

    private Integer page;

    private Integer startChar;

    private Integer endChar;

    private String content;

}
