package com.enterprise.aiassistant.backend.ai.chat.message.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageSourceResponse {

    private Long chunkId;

    private Long documentChunkId;

    private String documentTitle;

    private Integer pageNumber;

    private Double score;

    // Chunk text — lets the frontend show the same "View Evidence" content view
    // used by Semantic Search (EvidenceDialog) when a user clicks a source.
    private String content;

}
