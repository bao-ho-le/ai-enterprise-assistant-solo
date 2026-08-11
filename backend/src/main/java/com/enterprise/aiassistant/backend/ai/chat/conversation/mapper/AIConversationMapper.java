package com.enterprise.aiassistant.backend.ai.chat.conversation.mapper;

import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.*;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.TriggerGenerationResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.Generation;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.AIMessageResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AIConversationMapper {

    public StartDocumentQaConversationResponse toStartDocumentQaConversationResponse(
            ConversationResponse conversation,
            MessageResponse message
    ) {
        return StartDocumentQaConversationResponse.builder()
                .conversation(conversation)
                .message(message)
                .build();
    }

    public StartGenerationConversationResponse toStartGenerationConversationResponse(
            ConversationResponse conversation,
            TriggerGenerationResponse generation
    ) {
        return StartGenerationConversationResponse.builder()
                .conversation(conversation)
                .generation(generation)
                .build();
    }

    public DocumentQaConversationDetailResponse.AttachedDocumentItem toAttachedDocumentItem(
            AIConversationDocument conversationDocument
    ) {
        DocumentVersion version = conversationDocument.getDocumentVersion();

        return DocumentQaConversationDetailResponse.AttachedDocumentItem.builder()
                .documentVersionId(version.getId())
                .documentId(version.getDocument().getId())
                .documentTitle(version.getDocument().getTitle())
                .versionNumber(version.getVersionNumber())
                .build();
    }

    public AttachDocumentsResponse toAttachDocumentsResponse(List<AIConversationDocument> conversationDocuments) {
        return AttachDocumentsResponse.builder()
                .documents(conversationDocuments.stream().map(this::toAttachedDocumentItem).toList())
                .build();
    }

    public DocumentQaConversationDetailResponse toDocumentQaDetailResponse(
            AIConversation conversation,
            List<ConversationDocumentResponse> attachedDocuments,
            List<AIMessageResponse> recentMessages,
            boolean hasMoreMessages,
            boolean hasDeletedAttachedDocuments
    ) {
        return DocumentQaConversationDetailResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .conversationType(conversation.getConversationType())
                .status(conversation.getStatus())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .attachedDocuments(attachedDocuments)
                .recentMessages(recentMessages)
                .hasMoreMessages(hasMoreMessages)
                .hasDeletedAttachedDocuments(hasDeletedAttachedDocuments)
                .build();
    }

    public GenerationConversationDetailResponse toGenerationDetailResponse(
            AIConversation conversation,
            Generation generation,
            List<ConversationDocumentResponse> attachedDocuments,
            boolean hasDeletedAttachedDocuments
    ) {
        return GenerationConversationDetailResponse.builder()
                .generationId(generation.getId())
                .conversationId(conversation.getId())
                .title(conversation.getTitle())
                .conversationType(conversation.getConversationType())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .runCreatedAt(generation.getCreatedAt())
                .runUpdatedAt(generation.getUpdatedAt())
                .status(generation.getStatus())
                .inputData(generation.getInputData() != null ? generation.getInputData().toString() : null)
                .attachedDocuments(attachedDocuments)
                .generatedContentId(
                        generation.getGeneratedContent() != null
                                ? generation.getGeneratedContent().getId()
                                : null
                )
                .hasDeletedAttachedDocuments(hasDeletedAttachedDocuments)
                .build();
    }

    public AIConversation toEntity(CreateConversationRequest request, User owner) {

        return AIConversation.builder()
                .title(request.getTitle())
                .conversationType(request.getConversationType())
                .user(owner)
                .build();
    }

    public ConversationResponse toResponse(AIConversation conversation) {

        return ConversationResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .conversationType(conversation.getConversationType())
                .status(conversation.getStatus())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .deletedAt(conversation.getDeletedAt())
                .build();
    }

    public AIConversationDocument toConversationDocument(
            AIConversation conversation,
            DocumentVersion documentVersion
    ) {

        return AIConversationDocument.builder()
                .conversation(conversation)
                .documentVersion(documentVersion)
                .build();
    }

    public ConversationDocumentResponse toConversationDocumentResponse(
            AIConversationDocument conversationDocument) {

        return ConversationDocumentResponse.builder()
                .documentId(conversationDocument.getDocumentVersion().getDocument().getId())
                .documentVersionId(conversationDocument.getDocumentVersion().getId())
                .documentTitle(conversationDocument.getDocumentVersion().getDocument().getTitle())
                .versionNumber(conversationDocument.getDocumentVersion().getVersionNumber())
                .attachedAt(conversationDocument.getCreatedAt())
                .build();
    }
}
