package com.enterprise.aiassistant.backend.processing.chunking.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk {

    // Thứ tự của chunk này trong tài liệu
    private Integer chunkIndex;

    private String content;

    private Integer pageNumber;

    private Integer startChar;

    private Integer endChar;

    private Integer tokenCount;
}
