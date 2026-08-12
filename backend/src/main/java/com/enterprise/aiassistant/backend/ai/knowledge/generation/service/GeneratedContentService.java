package com.enterprise.aiassistant.backend.ai.knowledge.generation.service;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.request.UpdateGeneratedContentRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.GeneratedContentDetailResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.GeneratedContentResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedDocumentType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface GeneratedContentService {

    Slice<GeneratedContentResponse> getGeneratedContents(
            GeneratedDocumentType generatedType,
            Pageable pageable
    );

    GeneratedContentDetailResponse getGeneratedContentById(Long generatedContentId);

    GeneratedContentDetailResponse updateGeneratedContent(
            Long generatedContentId,
            UpdateGeneratedContentRequest request
    );
}

