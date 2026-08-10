package com.enterprise.aiassistant.backend.ai.knowledge.search.controller;

import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.response.SemanticSearchResult;
import com.enterprise.aiassistant.backend.ai.knowledge.search.service.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/search")
@RequiredArgsConstructor
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;

    @GetMapping("/semantic")
    public List<SemanticSearchResult> semanticSearch(SemanticSearchRequest request) {
        return semanticSearchService.search(request);
    }

}
