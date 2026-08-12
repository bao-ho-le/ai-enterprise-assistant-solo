package com.enterprise.aiassistant.backend.ai.knowledge.search.helper;

import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.analytics.usage.service.AIUsageLogService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.SearchException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchHelper {

    private static final int DEFAULT_TOP_K = 10;
    private static final int MAX_TOP_K = 50;
    private static final int MAX_KEYWORD_LENGTH = 255;

    private final AIUsageLogService aiUsageLogService;

    public void validateSearchRequest(SemanticSearchRequest request) {

        if (request == null
                || request.getKeyword() == null
                || request.getKeyword().isBlank()) {

            throw new SearchException(ErrorCode.SEARCH_KEYWORD_REQUIRED);
        }

        if (request.getKeyword().length() > MAX_KEYWORD_LENGTH) {
            throw new SearchException(ErrorCode.KEYWORD_TOO_LONG);
        }

        if (request.getTopK() != null
                && (request.getTopK() < 1 || request.getTopK() > MAX_TOP_K)) {

            throw new SearchException(ErrorCode.INVALID_TOP_K);
        }

        if (request.getDocumentId() != null && request.getDocumentId() <= 0) {
            throw new SearchException(ErrorCode.INVALID_DOCUMENT_ID);
        }
    }

    public int resolveTopK(Integer topK) {
        return topK != null ? topK : DEFAULT_TOP_K;
    }

    public void logUsage(String model, Integer inputTokens, AIUsageStatus status, String errorMessage) {
        aiUsageLogService.logAiUsage(AIUsageLogRequest.builder()
                .conversationType(ConversationType.SEMANTIC_SEARCH)
                .model(model)
                .inputTokens(inputTokens)
                .outputTokens(0)
                .status(status)
                .errorMessage(errorMessage)
                .build());
    }

}
