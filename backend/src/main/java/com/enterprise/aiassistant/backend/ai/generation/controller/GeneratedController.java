package com.enterprise.aiassistant.backend.ai.generation.controller;

import com.enterprise.aiassistant.backend.ai.generation.dto.request.UpdateGeneratedContentRequest;
import com.enterprise.aiassistant.backend.ai.generation.dto.response.GeneratedContentDetailResponse;
import com.enterprise.aiassistant.backend.ai.generation.dto.response.GeneratedContentResponse;
import com.enterprise.aiassistant.backend.ai.generation.enums.GeneratedDocumentType;
import com.enterprise.aiassistant.backend.ai.generation.service.GeneratedContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/generated-contents")
@RequiredArgsConstructor
public class GeneratedController {

    private final GeneratedContentService generatedContentService;

    // 3 Controller bên dưới hiện đang không sử dụng, trang generated content tại frontend đang bị ẩn đi

    @GetMapping
    public ResponseEntity<Slice<GeneratedContentResponse>> getGeneratedContents(
            @RequestParam(required = false) GeneratedDocumentType generatedType,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                generatedContentService.getGeneratedContents(generatedType, pageable)
        );
    }


    @GetMapping("/{generatedContentId}")
    public ResponseEntity<GeneratedContentDetailResponse> getGeneratedContentById(
            @PathVariable Long generatedContentId
    ) {
        return ResponseEntity.ok(
                generatedContentService.getGeneratedContentById(generatedContentId)
        );
    }


    @PutMapping("/{generatedContentId}")
    public ResponseEntity<GeneratedContentDetailResponse> updateGeneratedContent(
            @PathVariable Long generatedContentId,
            @Valid @RequestBody UpdateGeneratedContentRequest request
    ) {
        return ResponseEntity.ok(
                generatedContentService.updateGeneratedContent(generatedContentId, request)
        );
    }
}






