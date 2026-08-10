package com.enterprise.aiassistant.backend.ai.conversation.controller;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.*;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.*;
import com.enterprise.aiassistant.backend.ai.conversation.service.AIConversationService;
import com.enterprise.aiassistant.backend.ai.generation.dto.response.GenerationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/ai-conversations")
@RequiredArgsConstructor
public class AIConversationController {

    private final AIConversationService conversationService;


    @PostMapping("/document-qa/start")
    public ResponseEntity<StartDocumentQaConversationResponse> startDocumentQaConversation(
            @Valid @RequestBody StartDocumentQaConversationRequest request
    ) {
        return ResponseEntity.ok(conversationService.startDocumentQaConversation(request));
    }

    @PostMapping("/generation/start")
    public ResponseEntity<StartGenerationConversationResponse> startGenerationConversation(
            @Valid @RequestBody StartGenerationConversationRequest request
    ) {
        return ResponseEntity.ok(conversationService.startGenerationConversation(request));
    }

    @PutMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> renameConversation(
            @PathVariable Long conversationId,
            @Valid @RequestBody RenameConversationRequest request
    ) {
        return ResponseEntity.ok(conversationService.renameConversation(conversationId, request));
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> softDeleteConversation(@PathVariable Long conversationId) {
        conversationService.softDeleteConversation(conversationId);
        return ResponseEntity.noContent().build();
    }

    // Restore a soft-deleted conversation (status DELETED -> ACTIVE)
    // Returns the restored conversation so the client can refresh its lists
    @PostMapping("/{conversationId}/restore")
    public ResponseEntity<ConversationResponse> restoreConversation(@PathVariable Long conversationId) {
        return ResponseEntity.ok(conversationService.restoreConversation(conversationId));
    }

    @DeleteMapping("/{conversationId}/hard")
    public ResponseEntity<Void> hardDeleteConversation(@PathVariable Long conversationId) {
        conversationService.hardDeleteConversation(conversationId);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{conversationId}/documents")
    public ResponseEntity<AttachDocumentsResponse> attachDocuments(
            @PathVariable Long conversationId,
            @Valid @RequestBody AttachDocumentsRequest request
    ) {
        return ResponseEntity.ok(conversationService.attachDocuments(conversationId, request));
    }

    @DeleteMapping("/{conversationId}/documents/{documentVersionId}")
    public ResponseEntity<Void> removeDocument(
            @PathVariable Long conversationId,
            @PathVariable Long documentVersionId
    ) {
        conversationService.removeDocument(conversationId, documentVersionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{conversationId}")
    public DocumentQaConversationDetailResponse getDocumentQaConversationDetail(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "20") int recentMessagesLimit
    ) {
        return conversationService.getDocumentQaConversationDetail(conversationId, recentMessagesLimit);
    }

    @GetMapping
    public Slice<ConversationResponse> getConversations(
            ConversationFilterRequest filter,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return conversationService.getConversations(filter, pageable);
    }

    // Literal path, declared before /{conversationId} style lookups — soft-deleted only.
    @GetMapping("/deleted")
    public Slice<ConversationResponse> getDeletedConversations(
            ConversationFilterRequest filter,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return conversationService.getDeletedConversations(filter, pageable);
    }

    @GetMapping("/{conversationId}/generation-detail")
    public GenerationConversationDetailResponse getGenerationConversationDetail(
            @PathVariable Long conversationId
    ) {
        return conversationService.getGenerationConversationDetail(conversationId);
    }


    @GetMapping("/{conversationId}/documents")
    public ResponseEntity<List<ConversationDocumentResponse>> getConversationDocuments(
            @PathVariable Long conversationId) {

        return ResponseEntity.ok(
                conversationService.getConversationDocuments(conversationId)
        );
    }


    @GetMapping("/{conversationId}/generations")
    public ResponseEntity<Slice<GenerationResponse>> getConversationGenerations(
            @PathVariable Long conversationId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                conversationService.getConversationGenerations(conversationId, pageable)
        );
    }
}
