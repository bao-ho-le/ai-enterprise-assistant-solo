package com.enterprise.aiassistant.backend.ai.chat.conversation.service;

import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.service.DocumentAuthorizationService;
import java.util.Set;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.*;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.*;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.chat.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.conversation.mapper.AIConversationMapper;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.request.TriggerGenerationRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.GenerationResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.TriggerGenerationResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.Generation;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.mapper.GeneratedMapper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GeneratedContentRepository;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GenerationRepository;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.service.GenerationService;
import com.enterprise.aiassistant.backend.ai.chat.memory.repository.ConversationMemoryRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessagePageResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.service.AIMessageService;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.analytics.usage.repository.AIUsageLogRepository;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;


@Service
@RequiredArgsConstructor
public class AIConversationServiceImpl implements AIConversationService {

    // Mirror của AIConversationHelper.GENERATION_CONVERSATION_TYPES (private ở đó) —
    // dùng riêng để quyết định permission theo conversationType lúc tạo.
    private static final Set<ConversationType> GENERATION_CONVERSATION_TYPES = EnumSet.of(
            ConversationType.EMAIL_GENERATION,
            ConversationType.REPORT_GENERATION,
            ConversationType.SUMMARY_GENERATION,
            ConversationType.MEETING_MINUTES_GENERATION,
            ConversationType.FORM_GENERATION
    );

    private final CurrentUserService currentUserService;

    private final AIConversationRepository conversationRepository;

    private final DocumentAuthorizationService documentAuthorizationService;

    private final AIConversationDocumentRepository conversationDocumentRepository;

    private final AIMessageRepository messageRepository;

    private final AIMessageSourceRepository messageSourceRepository;

    private final ConversationMemoryRepository conversationMemoryRepository;

    private final DocumentVersionRepository documentVersionRepository;

    private final GeneratedContentRepository generatedContentRepository;

    private final GenerationRepository generationRepository;

    private final AIUsageLogRepository usageLogRepository;

    private final AIConversationMapper aiConversationMapper;

    private final AIConversationHelper aiConversationHelper;

    private final GeneratedMapper generatedMapper;

    private final AIMessageService aiMessageService;

    private final GenerationService generationService;


    @Override
    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {

        aiConversationHelper.validateCreateConversationRequest(request);

        currentUserService.requirePermission(Permission.CONVERSATION_CREATE);
        requireFeaturePermission(request.getConversationType());

        AIConversation conversation = aiConversationMapper.toEntity(
                request,
                currentUserService.getCurrentUser()
        );
        conversationRepository.save(conversation);

        return aiConversationMapper.toResponse(conversation);
    }


    @Override
    @Transactional
    public StartDocumentQaConversationResponse startDocumentQaConversation(StartDocumentQaConversationRequest request) {

        aiConversationHelper.validateStartDocumentQaRequest(request);

        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setConversationType(ConversationType.DOCUMENT_QA);
        ConversationResponse conversation = createConversation(createRequest);

        AttachDocumentsRequest attachRequest = new AttachDocumentsRequest();
        attachRequest.setDocumentVersionIds(request.getDocumentVersionIds());
        attachDocuments(conversation.getId(), attachRequest);

        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setContent(request.getContent());
        MessageResponse message = aiMessageService.sendMessage(conversation.getId(), sendMessageRequest);

        return aiConversationMapper.toStartDocumentQaConversationResponse(
                conversation,
                message
        );
    }


    @Override
    @Transactional
    public StartGenerationConversationResponse startGenerationConversation(StartGenerationConversationRequest request) {

        aiConversationHelper.validateStartGenerationRequest(request);
        aiConversationHelper.validateGenerationConversationType(request.getConversationType());

        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setConversationType(request.getConversationType());
        ConversationResponse conversation = createConversation(createRequest);

        if (request.getDocumentVersionIds() != null && !request.getDocumentVersionIds().isEmpty()) {
            AttachDocumentsRequest attachRequest = new AttachDocumentsRequest();
            attachRequest.setDocumentVersionIds(request.getDocumentVersionIds());
            attachDocuments(conversation.getId(), attachRequest);
        }

        TriggerGenerationRequest triggerRequest = new TriggerGenerationRequest();
        triggerRequest.setInputData(request.getInputData());
        TriggerGenerationResponse generation = generationService.generate(conversation.getId(), triggerRequest);

        return aiConversationMapper.toStartGenerationConversationResponse(
                conversation,
                generation
        );
    }


    @Override
    @Transactional
    public ConversationResponse renameConversation(Long conversationId, RenameConversationRequest request) {

        aiConversationHelper.validateRenameRequest(conversationId, request);

        currentUserService.requirePermission(Permission.CONVERSATION_UPDATE);

        AIConversation conversation = getOwnedConversationOrThrow(conversationId, ConversationStatus.ACTIVE);

        conversation.setTitle(request.getTitle());
        conversationRepository.save(conversation);

        return aiConversationMapper.toResponse(conversation);
    }


    @Override
    @Transactional
    public void softDeleteConversation(Long conversationId) {

        aiConversationHelper.validateConversationId(conversationId);

        currentUserService.requirePermission(Permission.CONVERSATION_DELETE);

        AIConversation conversation = getOwnedConversationOrThrow(conversationId, ConversationStatus.ACTIVE);

        conversation.setStatus(ConversationStatus.DELETED);
        conversation.setDeletedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public ConversationResponse restoreConversation(Long conversationId) {

        aiConversationHelper.validateConversationId(conversationId);

        // Restore là đảo ngược của (soft) delete, nên dùng chung CONVERSATION_DELETE,
        // đồng bộ với softDeleteConversation/hardDeleteConversation.
        currentUserService.requirePermission(Permission.CONVERSATION_DELETE);

        // Distinguish the two failure cases the requirement calls out: a missing
        // conversation (404) vs one that exists but isn't soft-deleted (400).
        AIConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        aiConversationHelper.validateOwnership(conversation, currentUserService.getCurrentUserId());

        if (conversation.getStatus() != ConversationStatus.DELETED) {
            throw new AIConversationException(ErrorCode.CONVERSATION_NOT_DELETED);
        }

        // Un-soft-delete in place: the same row flips back to ACTIVE with its
        // history (messages/documents/generations) untouched, not a new conversation.
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setDeletedAt(null);
        conversationRepository.save(conversation);

        return aiConversationMapper.toResponse(conversation);
    }

    @Override
    @Transactional
    public void hardDeleteConversation(Long conversationId) {

        aiConversationHelper.validateConversationId(conversationId);

        currentUserService.requirePermission(Permission.CONVERSATION_DELETE);

        // Hard delete must reach conversations already soft-deleted too, so no status filter here.
        AIConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        aiConversationHelper.validateOwnership(conversation, currentUserService.getCurrentUserId());

        // GeneratedContent no longer has its own ai_conversation_id (item 4) - collect its
        // ids through the conversation's Generations before those rows are deleted.
        List<Long> generatedContentIds = generationRepository.findByAiConversationId(conversationId).stream()
                .map(Generation::getGeneratedContent)
                .filter(Objects::nonNull)
                .map(GeneratedContent::getId)
                .toList();

        messageSourceRepository.deleteByAiMessage_ConversationId(conversationId);
        messageRepository.deleteByConversationId(conversationId);
        conversationMemoryRepository.deleteByConversationId(conversationId);
        conversationDocumentRepository.deleteByConversationId(conversationId);
        generationRepository.deleteByAiConversationId(conversationId);
        if (!generatedContentIds.isEmpty()) {
            generatedContentRepository.deleteAllById(generatedContentIds);
        }
        usageLogRepository.deleteByAiConversationId(conversationId);
        conversationRepository.delete(conversation);
    }

    @Override
    @Transactional
    public AttachDocumentsResponse attachDocuments(Long conversationId, AttachDocumentsRequest request) {

        // Validate request data
        aiConversationHelper.validateAttachRequest(conversationId, request);

        // Attach = sửa đổi context của conversation, cùng nhóm quyền với renameConversation.
        currentUserService.requirePermission(Permission.CONVERSATION_UPDATE);

        // Find active conversation
        AIConversation conversation = getOwnedConversationOrThrow(conversationId, ConversationStatus.ACTIVE);

        // Remove duplicate document IDs
        List<Long> documentVersionIds = request.getDocumentVersionIds().stream().distinct().toList();

        // Load document versions
        List<DocumentVersion> versions = documentVersionRepository.findAllById(documentVersionIds);

        // Ensure all requested documents exist
        if (versions.size() != documentVersionIds.size()) {
            throw new DocumentException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND);
        }

        // Chặn attach document đã bị soft-delete
        aiConversationHelper.validateVersionsNotDeleted(versions);

        // Không cho kéo tài liệu ngoài quyền đọc vào context của AI.
        requireReadableDocumentVersions(versions);

        // Get already attached documents
        List<Long> alreadyAttachedIds =
                conversationDocumentRepository.findDocumentVersionIdsByConversationId(conversationId);

        // Filter out documents that are already attached
        List<DocumentVersion> newVersions =
                aiConversationHelper.filterNewVersions(versions, alreadyAttachedIds);

        // Create conversation-document mappings
        List<AIConversationDocument> newLinks = newVersions.stream()
                .map(version -> aiConversationMapper.toConversationDocument(conversation, version))
                .toList();

        // Save new document attachments
        conversationDocumentRepository.saveAll(newLinks);

        // Load all attached documents
        List<AIConversationDocument> allLinks =
                conversationDocumentRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        // Build response
        return aiConversationMapper.toAttachDocumentsResponse(allLinks);
    }

    @Override
    @Transactional
    public void removeDocument(Long conversationId, Long documentVersionId) {

        currentUserService.requirePermission(Permission.CONVERSATION_UPDATE);

        getActiveConversationOrThrow(conversationId);

        AIConversationDocument conversationDocument = conversationDocumentRepository
                .findByConversationIdAndDocumentVersionId(conversationId, documentVersionId)
                .orElseThrow(() -> new ConversationException(ErrorCode.DOCUMENT_NOT_ATTACHED_TO_CONVERSATION));

        conversationDocumentRepository.delete(conversationDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentQaConversationDetailResponse getDocumentQaConversationDetail(
            Long conversationId,
            int recentMessagesLimit
    ) {

        aiConversationHelper.validateConversationId(conversationId);
        aiConversationHelper.validateRecentMessagesLimit(recentMessagesLimit);

        currentUserService.requirePermission(Permission.CONVERSATION_READ);

        AIConversation conversation = getOwnedConversationOrThrow(conversationId, ConversationStatus.ACTIVE);

        List<ConversationDocumentResponse> attachedDocuments = getConversationDocuments(conversationId);
        boolean hasDeletedAttachedDocuments = hasDeletedAttachedDocuments(conversationId);

        // beforeId=null -> the latest `recentMessagesLimit` messages (see AIMessageServiceImpl),
        // not the oldest — a conversation longer than the limit must open showing its tail end.
        MessagePageResponse recentMessages = aiMessageService.getMessages(
                conversationId,
                null,
                recentMessagesLimit
        );

        return aiConversationMapper.toDocumentQaDetailResponse(
                conversation,
                attachedDocuments,
                recentMessages.getContent(),
                recentMessages.isHasMore(),
                hasDeletedAttachedDocuments
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ConversationResponse> getConversations(
            ConversationFilterRequest filter,
            Pageable pageable
    ) {
        currentUserService.requirePermission(Permission.CONVERSATION_READ);

        ConversationStatus status = filter.getStatus() != null ? filter.getStatus() : ConversationStatus.ACTIVE;

        return conversationRepository.filterConversations(
                filter.getConversationType(),
                status,
                currentUserService.getCurrentUserId(),
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ConversationResponse> getDeletedConversations(
            ConversationFilterRequest filter,
            Pageable pageable
    ) {
        currentUserService.requirePermission(Permission.CONVERSATION_READ);

        return conversationRepository.filterDeletedConversations(
                filter.getConversationType(),
                currentUserService.getCurrentUserId(),
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GenerationConversationDetailResponse getGenerationConversationDetail(Long conversationId) {

        aiConversationHelper.validateConversationId(conversationId);

        currentUserService.requirePermission(Permission.CONVERSATION_READ);

        AIConversation conversation = getOwnedConversationOrThrow(conversationId, ConversationStatus.ACTIVE);

        aiConversationHelper.validateGenerationConversationType(conversation.getConversationType());

        Generation generation = generationRepository
                .findFirstByAiConversationIdOrderByCreatedAtDesc(conversationId)
                .orElseThrow(() -> new AIConversationException(ErrorCode.GENERATION_NOT_FOUND));

        // Nếu conversation type là email thì không có attach document
        boolean isEmailGeneration = conversation.getConversationType() == ConversationType.EMAIL_GENERATION;
        List<ConversationDocumentResponse> attachedDocuments =
                isEmailGeneration ? null : getConversationDocuments(conversationId);
        boolean hasDeletedAttachedDocuments = !isEmailGeneration && hasDeletedAttachedDocuments(conversationId);

        return aiConversationMapper.toGenerationDetailResponse(
                conversation,
                generation,
                attachedDocuments,
                hasDeletedAttachedDocuments
        );
    }

    @Override
    public List<ConversationDocumentResponse> getConversationDocuments(Long conversationId) {

        currentUserService.requirePermission(Permission.CONVERSATION_READ);

        getActiveConversationOrThrow(conversationId);

        return conversationDocumentRepository
                .findByAiConversationIdWithDocument(conversationId)
                .stream()
                .map(aiConversationMapper::toConversationDocumentResponse)
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public Slice<GenerationResponse> getConversationGenerations(
            Long conversationId,
            Pageable pageable
    ) {

        currentUserService.requirePermission(Permission.CONVERSATION_READ);

        getActiveConversationOrThrow(conversationId);

        return generationRepository
                .findByAiConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(generatedMapper::toGenerationResponse);
    }


    // Helper

    // Permission theo tính năng AI cụ thể, tách khỏi CONVERSATION_CREATE (RBAC tạo conversation
    // nói chung). DOCUMENT_QA/generation type mới cần — SEMANTIC_SEARCH, DOCUMENT_INDEXING
    // không tạo qua đường này (xem AIConversationHelper.GENERATION_CONVERSATION_TYPES) nên bỏ qua.
    private void requireFeaturePermission(ConversationType conversationType) {

        if (conversationType == ConversationType.DOCUMENT_QA) {
            currentUserService.requirePermission(Permission.AI_DOCUMENT_QA);
            return;
        }

        if (GENERATION_CONVERSATION_TYPES.contains(conversationType)) {
            currentUserService.requirePermission(Permission.AI_DOCUMENT_GENERATION);
        }
    }

    // Mọi đường vào conversation đều đi qua đây nên ownership check đặt ở một chỗ duy nhất.
    private void getActiveConversationOrThrow(Long conversationId) {
        getOwnedConversationOrThrow(conversationId, ConversationStatus.ACTIVE);
    }

    private AIConversation getOwnedConversationOrThrow(Long conversationId, ConversationStatus status) {

        AIConversation conversation = conversationRepository.findByIdAndStatus(conversationId, status)
                .orElseThrow(() -> new ConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        aiConversationHelper.validateOwnership(conversation, currentUserService.getCurrentUserId());

        return conversation;
    }

    // AI chỉ được đọc tài liệu mà chính user có quyền đọc — chặn ngay từ bước attach.
    private void requireReadableDocumentVersions(List<DocumentVersion> versions) {

        List<Document> documents = versions.stream()
                .map(DocumentVersion::getDocument)
                .filter(Objects::nonNull)
                .toList();

        Set<Long> readableDocumentIds =
                documentAuthorizationService.filterReadableDocumentIds(documents);

        boolean allReadable = documents.stream()
                .allMatch(document -> readableDocumentIds.contains(document.getId()));

        if (!allReadable) {
            throw new AuthorizationException(ErrorCode.ACCESS_DENIED);
        }
    }

    private boolean hasDeletedAttachedDocuments(Long conversationId) {
        return conversationDocumentRepository
                .existsByConversationIdAndDocumentVersionDocumentStatus(conversationId, DocumentStatus.DELETED);
    }
}



