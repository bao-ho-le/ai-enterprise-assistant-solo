package com.enterprise.aiassistant.backend.ai.search.service;

import com.enterprise.aiassistant.backend.ai.embedding.dto.EmbeddingResult;
import com.enterprise.aiassistant.backend.ai.embedding.service.EmbeddingService;
import com.enterprise.aiassistant.backend.ai.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.ai.search.dto.response.SemanticSearchResult;
import com.enterprise.aiassistant.backend.ai.search.helper.SearchHelper;
import com.enterprise.aiassistant.backend.ai.search.mapper.SearchMapper;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.vectorstore.service.VectorStoreService;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
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

    @Override
    @Transactional(readOnly = true)
    public List<SemanticSearchResult> search(SemanticSearchRequest request) {

        searchHelper.validateSearchRequest(request);

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

    // Chỉ lấy các document không bị delete
    private Map<Long, Document> fetchActiveDocuments(List<SearchResult> hits) {

        Set<Long> documentIds = hits.stream()
                .map(hit -> hit.getPayload().getDocumentId())
                .collect(Collectors.toSet());

        return documentRepository.findAllById(documentIds).stream()
                .filter(document -> document.getStatus() == DocumentStatus.ACTIVE)
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
