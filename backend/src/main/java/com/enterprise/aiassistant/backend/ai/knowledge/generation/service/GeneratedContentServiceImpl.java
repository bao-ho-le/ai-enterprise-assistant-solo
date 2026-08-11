package com.enterprise.aiassistant.backend.ai.knowledge.generation.service;


import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.request.UpdateGeneratedContentRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.GeneratedContentDetailResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.GeneratedContentResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedDocumentType;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.helper.GeneratedHelper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.mapper.GeneratedMapper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GeneratedContentRepository;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.GeneratedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GeneratedContentServiceImpl implements GeneratedContentService {

    private final GeneratedContentRepository generatedContentRepository;
    private final GeneratedMapper generatedMapper;
    private final GeneratedHelper generatedHelper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public Slice<GeneratedContentResponse> getGeneratedContents(
            GeneratedDocumentType generatedType,
            Pageable pageable
    ) {
        return generatedContentRepository
                .findOwnedByUser(
                        currentUserService.getCurrentUserId(),
                        generatedType,
                        pageable
                )
                .map(generatedMapper::toGeneratedContentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public GeneratedContentDetailResponse getGeneratedContentById(Long generatedContentId) {
        generatedHelper.validateGeneratedContentId(generatedContentId);

        GeneratedContent generatedContent = getGeneratedContentOrThrow(generatedContentId);

        return generatedMapper.toGeneratedContentDetailResponse(generatedContent);
    }

    @Override
    @Transactional
    public GeneratedContentDetailResponse updateGeneratedContent(
            Long generatedContentId,
            UpdateGeneratedContentRequest request
    ) {
        generatedHelper.validateGeneratedContentId(generatedContentId);
        generatedHelper.validateUpdateRequest(request);

        GeneratedContent generatedContent = getGeneratedContentOrThrow(generatedContentId);

        generatedContent.setTitle(request.getTitle().trim());
        generatedContent.setContent(request.getContent().trim());

        return generatedMapper.toGeneratedContentDetailResponse(generatedContent);
    }

    // Helper

    private GeneratedContent getGeneratedContentOrThrow(Long generatedContentId) {

        GeneratedContent generatedContent = generatedContentRepository.findById(generatedContentId)
                .orElseThrow(() -> new GeneratedException(ErrorCode.GENERATED_CONTENT_NOT_FOUND));

        if (!generatedContentRepository.isOwnedByUser(
                generatedContentId, currentUserService.getCurrentUserId())) {

            throw new AuthorizationException(ErrorCode.ACCESS_DENIED);
        }

        return generatedContent;
    }
}

