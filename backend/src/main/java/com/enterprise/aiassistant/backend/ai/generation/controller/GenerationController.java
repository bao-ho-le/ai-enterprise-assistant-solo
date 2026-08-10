package com.enterprise.aiassistant.backend.ai.generation.controller;

import com.enterprise.aiassistant.backend.ai.conversation.dto.response.GenerationConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.generation.dto.request.TriggerGenerationRequest;
import com.enterprise.aiassistant.backend.ai.generation.dto.response.TriggerGenerationResponse;
import com.enterprise.aiassistant.backend.ai.generation.service.GenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}")
@RequiredArgsConstructor
public class GenerationController {

    private final GenerationService generationService;

    // Api này hiện đang dùng cho generate lần đầu và cả regenerate
    @PostMapping("/ai-conversations/{conversationId}/generate")
    public TriggerGenerationResponse generate(
            @PathVariable Long conversationId,
            @Valid @RequestBody TriggerGenerationRequest request
    ) {
        return generationService.generate(conversationId, request);
    }


    // Api này hiện đang không được dùng, lấy chi tiết lịch sử generation hiện đang được viết ở Ai Conversation
    @GetMapping("/ai-conversations/generations/{generationId}")
    public GenerationConversationDetailResponse getGenerationDetail(
            @PathVariable Long generationId
    ) {
        return generationService.getGenerationDetail(generationId);
    }
}
