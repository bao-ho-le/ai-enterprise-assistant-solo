package com.enterprise.aiassistant.backend.processing.orchestration.mapper;

import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.dto.EmbeddingResult;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPoint;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import com.enterprise.aiassistant.backend.document.entity.DocumentText;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.processing.chunking.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import org.springframework.stereotype.Component;


@Component
public class ProcessingMapper {

    public DocumentText toDocumentText(DocumentVersion version, ExtractedText extractedText) {
        return DocumentText.builder()
                .documentVersion(version)
                .content(extractedText.getContent())
                .extractionMethod(extractedText.getExtractionMethod())
                .language(extractedText.getLanguage())
                .build();
    }

    public DocumentChunk toDocumentChunk(DocumentVersion version, TextChunk textChunk) {
        return DocumentChunk.builder()
                .documentVersion(version)
                .chunkIndex(textChunk.getChunkIndex())
                .content(textChunk.getContent())
                .pageNumber(textChunk.getPageNumber())
                .startChar(textChunk.getStartChar())
                .endChar(textChunk.getEndChar())
                .tokenCount(textChunk.getTokenCount())
                .build();
    }

    public VectorPoint toVectorPoint(DocumentChunk chunk, EmbeddingResult embeddingResult) {

        VectorPayload payload = VectorPayload.builder()
                .chunkId(chunk.getId())
                .documentId(chunk.getDocumentVersion().getDocument().getId())
                .documentVersionId(chunk.getDocumentVersion().getId())
                .chunkIndex(chunk.getChunkIndex())
                .pageNumber(chunk.getPageNumber())
                .startChar(chunk.getStartChar())
                .endChar(chunk.getEndChar())
                .tokenCount(chunk.getTokenCount())
                .embeddingModel(embeddingResult.getModel())
                .content(chunk.getContent())
                .build();

        return VectorPoint.builder()
                .id(chunk.getId())
                .vector(embeddingResult.getVector())
                .payload(payload)
                .build();
    }
}
