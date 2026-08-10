package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.*;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.*;
import com.enterprise.aiassistant.backend.ai.generation.dto.response.GenerationResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;


public interface AIConversationService {

    ConversationResponse createConversation(CreateConversationRequest request);


    ConversationResponse renameConversation(Long conversationId, RenameConversationRequest request);


    void softDeleteConversation(Long conversationId);

    void hardDeleteConversation(Long conversationId);

    // Reverts a soft delete in place (status DELETED -> ACTIVE, clears deletedAt) —
    // never creates a new conversation. Only soft-deleted conversations can be restored.
    ConversationResponse restoreConversation(Long conversationId);

    AttachDocumentsResponse attachDocuments(Long conversationId, AttachDocumentsRequest request);

    // Atomic: create + attach + first message in one transaction, so a failure at any
    // step rolls back the whole thing — no orphaned empty conversation can result.
    StartDocumentQaConversationResponse startDocumentQaConversation(StartDocumentQaConversationRequest request);

    // Atomic: create + attach (if any) + generate in one transaction, so a failure at any
    // step rolls back the whole thing — no orphaned empty conversation can result.
    StartGenerationConversationResponse startGenerationConversation(StartGenerationConversationRequest request);


    Slice<ConversationResponse> getConversations(
            ConversationFilterRequest filter,
            Pageable pageable
    );

    // Soft-deleted conversations only — status is forced server-side, the filter's own
    // status field is ignored here.
    Slice<ConversationResponse> getDeletedConversations(
            ConversationFilterRequest filter,
            Pageable pageable
    );

    DocumentQaConversationDetailResponse getDocumentQaConversationDetail(Long conversationId, int recentMessagesLimit);

    GenerationConversationDetailResponse getGenerationConversationDetail(Long conversationId);

    List<ConversationDocumentResponse> getConversationDocuments(Long conversationId);

    void removeDocument(Long conversationId, Long documentVersionId);

    Slice<GenerationResponse> getConversationGenerations(Long conversationId, Pageable pageable);

}
