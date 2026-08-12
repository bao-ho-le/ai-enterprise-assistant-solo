package com.enterprise.aiassistant.backend.ai.knowledge.search.service;

import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.dto.EmbeddingResult;
import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.service.EmbeddingService;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.response.SemanticSearchResult;
import com.enterprise.aiassistant.backend.ai.knowledge.search.helper.SearchHelper;
import com.enterprise.aiassistant.backend.ai.knowledge.search.mapper.SearchMapper;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.service.VectorStoreService;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.document.service.DocumentAuthorizationService;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemanticSearchServiceImpl implements SemanticSearchService {

    private final EmbeddingService embeddingService;

    private final VectorStoreService vectorStoreService;

    private final DocumentRepository documentRepository;

    private final SearchHelper searchHelper;

    private final SearchMapper searchMapper;

    private final DocumentAuthorizationService documentAuthorizationService;

    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public List<SemanticSearchResult> search(SemanticSearchRequest request) {

        searchHelper.validateSearchRequest(request);

        currentUserService.requirePermission(Permission.AI_SEMANTIC_SEARCH);

        int topK = searchHelper.resolveTopK(request.getTopK());

        String model = embeddingService.getModelName();
        Integer inputTokens = null;

        try {
            EmbeddingResult queryEmbedding = embeddingService.embed(request.getKeyword());
            model = queryEmbedding.getModel();
            inputTokens = queryEmbedding.getInputTokens();

            List<SearchResult> hits = vectorStoreService.search(
                    queryEmbedding.getVector(),
                    topK,
                    request.getDocumentId()
            );

            List<SemanticSearchResult> results;
            if (hits.isEmpty()) {
                results = List.of();
            } else {
                Map<Long, Document> activeDocumentsById = fetchActiveDocuments(hits);

                List<SearchResult> validHits = hits.stream()
                        .filter(hit -> isCurrentVersionHit(hit, activeDocumentsById))
                        .toList();

                results = searchMapper.toSemanticSearchResults(validHits);
            }

            searchHelper.logUsage(model, inputTokens, AIUsageStatus.SUCCESS, null);
            return results;

        } catch (RuntimeException ex) {
            searchHelper.logUsage(model, inputTokens, AIUsageStatus.FAILED, ex.getMessage());
            throw ex;
        }
    }


    // Helper

    // Chỉ lấy document còn ACTIVE và user hiện tại có quyền đọc — Qdrant không tự biết
    // phân quyền nên phải lọc lại trước khi trả kết quả ra ngoài.
    private Map<Long, Document> fetchActiveDocuments(List<SearchResult> hits) {

        Set<Long> documentIds = hits.stream()
                .map(hit -> hit.getPayload().getDocumentId())
                .collect(Collectors.toSet());

        List<Document> activeDocuments = documentRepository.findAllById(documentIds).stream()
                .filter(document -> document.getStatus() == DocumentStatus.ACTIVE)
                .toList();

        Set<Long> readableDocumentIds =
                documentAuthorizationService.filterReadableDocumentIds(activeDocuments);

        return activeDocuments.stream()
                .filter(document -> readableDocumentIds.contains(document.getId()))
                .collect(Collectors.toMap(Document::getId, Function.identity()));
    }

    // Kiểm tra nếu current version giống với bản trong request thì mới cho search
    // Tránh lấy hits từ version cũ
    private boolean isCurrentVersionHit(SearchResult hit, Map<Long, Document> activeDocumentsById) {

        VectorPayload payload = hit.getPayload();
        Document document = activeDocumentsById.get(payload.getDocumentId());

        return document != null
                && document.getCurrentVersion() != null
                && document.getCurrentVersion().getId().equals(payload.getDocumentVersionId());
    }

}
