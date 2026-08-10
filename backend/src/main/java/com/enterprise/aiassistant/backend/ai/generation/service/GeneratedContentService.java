package com.enterprise.aiassistant.backend.ai.generation.service;

import com.enterprise.aiassistant.backend.ai.generation.dto.request.UpdateGeneratedContentRequest;
import com.enterprise.aiassistant.backend.ai.generation.dto.response.GeneratedContentDetailResponse;
import com.enterprise.aiassistant.backend.ai.generation.dto.response.GeneratedContentResponse;
import com.enterprise.aiassistant.backend.ai.generation.enums.GeneratedDocumentType;
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

