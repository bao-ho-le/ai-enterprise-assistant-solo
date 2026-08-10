package com.enterprise.aiassistant.backend.ai.generation.helper;

import com.enterprise.aiassistant.backend.ai.generation.dto.request.UpdateGeneratedContentRequest;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.GeneratedException;
import org.springframework.stereotype.Component;

@Component
public class GeneratedHelper {

    private static final int MAX_TITLE_LENGTH = 500;

    public void validateGeneratedContentId(Long generatedContentId) {
        if (generatedContentId == null) {
            throw new GeneratedException(ErrorCode.GENERATED_CONTENT_ID_REQUIRED);
        }

        if (generatedContentId <= 0) {
            throw new GeneratedException(ErrorCode.GENERATED_CONTENT_ID_INVALID);
        }
    }

    public void validateUpdateRequest(UpdateGeneratedContentRequest request) {
        if (request == null) {
            throw new GeneratedException(ErrorCode.GENERATED_CONTENT_UPDATE_REQUEST_REQUIRED);

        }

        validateTitle(request.getTitle());
        validateContent(request.getContent());
    }

    // Dùng cho updateGeneratedContent(...)
    public void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new GeneratedException(
                    ErrorCode.GENERATED_CONTENT_TITLE_REQUIRED
            );
        }

        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new GeneratedException(
                    ErrorCode.GENERATED_CONTENT_TITLE_TOO_LONG
            );
        }
    }

    // Dùng cho updateGeneratedContent(...)
    public void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new GeneratedException(
                    ErrorCode.GENERATED_CONTENT_BODY_REQUIRED
            );
        }
    }
}



