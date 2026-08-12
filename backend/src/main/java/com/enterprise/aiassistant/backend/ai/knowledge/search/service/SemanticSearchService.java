package com.enterprise.aiassistant.backend.ai.knowledge.search.service;

import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.response.SemanticSearchResult;

import java.util.List;

public interface SemanticSearchService {

    List<SemanticSearchResult> search(SemanticSearchRequest request);

}
