package com.enterprise.aiassistant.backend.ai.chat.message.controller;

import com.enterprise.aiassistant.backend.ai.chat.message.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessageDetailResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessagePageResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.service.AIMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/ai-conversations/{conversationId}/messages")
@RequiredArgsConstructor
public class AIMessageController {

    private final AIMessageService messageService;


    @PostMapping
    public MessageResponse sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return messageService.sendMessage(conversationId, request);
    }


    @GetMapping
    public MessagePageResponse getMessages(
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "20") int size
    ) {

        return messageService.getMessages(
                conversationId,
                beforeId,
                size
        );
    }

    @GetMapping("/{messageId}")
    public MessageDetailResponse getMessageEvidence(
            @PathVariable Long conversationId,
            @PathVariable Long messageId
    ) {
        return messageService.getMessageEvidence(conversationId, messageId);
    }
}
