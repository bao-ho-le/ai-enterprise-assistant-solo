package com.enterprise.aiassistant.backend.ai.knowledge.generation.mapper;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.GeneratedContentDetailResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.GeneratedContentResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.GenerationResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.Generation;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedDocumentType;
import org.springframework.stereotype.Component;

@Component
public class GeneratedMapper {

    public GeneratedContentResponse toGeneratedContentResponse(
            GeneratedContent generatedContent
    ) {
        return GeneratedContentResponse.builder()
                .id(generatedContent.getId())
                .aiConversationId(aiConversationId(generatedContent))
                .generatedType(generatedContent.getGeneratedType())
                .title(generatedContent.getTitle())
                .createdAt(generatedContent.getCreatedAt())
                .updatedAt(generatedContent.getUpdatedAt())
                .build();
    }

    public GeneratedContentDetailResponse toGeneratedContentDetailResponse(
            GeneratedContent generatedContent
    ) {
        return GeneratedContentDetailResponse.builder()
                .id(generatedContent.getId())
                .aiConversationId(aiConversationId(generatedContent))
                .generatedType(generatedContent.getGeneratedType())
                .title(generatedContent.getTitle())
                .content(generatedContent.getContent())
                .createdAt(generatedContent.getCreatedAt())
                .updatedAt(generatedContent.getUpdatedAt())
                .build();
    }

    public GeneratedContent toCreateGeneratedContentObject(
            GeneratedDocumentType generatedType,
            String title,
            String content
    ) {

        return GeneratedContent.builder()
                .generatedType(generatedType)
                .title(title.trim())
                .content(content.trim())
                .build();
    }

    // History list item (item 5): metadata only, never the content body.
    public GenerationResponse toGenerationResponse(Generation generation) {
        return GenerationResponse.builder()
                .generationId(generation.getId())
                .status(generation.getStatus())
                .createdAt(generation.getCreatedAt())
                .generatedContentId(
                        generation.getGeneratedContent() != null
                                ? generation.getGeneratedContent().getId()
                                : null
                )
                .build();
    }

    // GeneratedContent no longer holds ai_conversation_id directly (item 4: it's a child
    // of Generation, not of AIConversation) - reached through the owning Generation instead.
    private Long aiConversationId(GeneratedContent generatedContent) {
        return generatedContent.getGeneration() != null
                ? generatedContent.getGeneration().getAiConversation().getId()
                : null;
    }
}
