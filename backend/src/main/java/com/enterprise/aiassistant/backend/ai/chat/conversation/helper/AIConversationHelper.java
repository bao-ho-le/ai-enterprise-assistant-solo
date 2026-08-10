package com.enterprise.aiassistant.backend.ai.chat.conversation.helper;

import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.RenameConversationRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.StartDocumentQaConversationRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.StartGenerationConversationRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class AIConversationHelper {

    private static final int MIN_RECENT_MESSAGES_LIMIT = 1;
    private static final int MAX_RECENT_MESSAGES_LIMIT = 100;

    private static final DateTimeFormatter DEFAULT_TITLE_TIMESTAMP =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Set<ConversationType> GENERATION_CONVERSATION_TYPES = EnumSet.of(
            ConversationType.EMAIL_GENERATION,
            ConversationType.REPORT_GENERATION,
            ConversationType.SUMMARY_GENERATION,
            ConversationType.MEETING_MINUTES_GENERATION,
            ConversationType.FORM_GENERATION
    );


    public List<DocumentVersion> filterNewVersions(
            List<DocumentVersion> versions,
            List<Long> alreadyAttachedVersionIds
    ) {
        return versions.stream()
                .filter(version -> !alreadyAttachedVersionIds.contains(version.getId()))
                .toList();
    }

    public void validateCreateConversationRequest(CreateConversationRequest request) {

        if (request == null || request.getConversationType() == null) {
            throw new BusinessException(ErrorCode.REQUEST_REQUIRED);
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            request.setTitle(defaultTitle(request.getConversationType()));
        }
    }

    private String defaultTitle(ConversationType conversationType) {
        return displayName(conversationType) + " - " + LocalDateTime.now().format(DEFAULT_TITLE_TIMESTAMP);
    }

    private String displayName(ConversationType conversationType) {
        String[] words = conversationType.name().split("_");
        StringBuilder displayName = new StringBuilder();
        for (String word : words) {
            if (!displayName.isEmpty()) {
                displayName.append(" ");
            }
            displayName.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return displayName.toString();
    }


    public void validateRenameRequest(Long conversationId, RenameConversationRequest request) {
        validateConversationId(conversationId);
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException(ErrorCode.REQUEST_REQUIRED);
        }
    }

    public void validateAttachRequest(Long conversationId, AttachDocumentsRequest request) {
        validateConversationId(conversationId);
        if (request == null || request.getDocumentVersionIds() == null || request.getDocumentVersionIds().isEmpty()) {
            throw new BusinessException(ErrorCode.REQUEST_REQUIRED);
        }
    }

    // Chặn attach document đã bị soft-delete vào conversation
    public void validateVersionsNotDeleted(List<DocumentVersion> versions) {
        boolean hasDeletedDocument = versions.stream()
                .anyMatch(version -> version.getDocument().getStatus() == DocumentStatus.DELETED);

        if (hasDeletedDocument) {
            throw new DocumentException(ErrorCode.DOCUMENT_DELETED);
        }
    }

    // Dùng chung cho Document QA / Report / Summary: chặn build context và gọi LLM
    // khi có tài liệu đính kèm đã bị soft-delete sau khi attach.
    public void validateAttachedDocumentsNotDeleted(List<AIConversationDocument> attachedDocuments) {
        boolean hasDeletedDocument = attachedDocuments.stream()
                .anyMatch(link -> link.getDocumentVersion().getDocument().getStatus() == DocumentStatus.DELETED);

        if (hasDeletedDocument) {
            throw new AIConversationException(ErrorCode.ATTACHED_DOCUMENT_DELETED);
        }
    }

    public void validateConversationId(Long conversationId) {

        if (conversationId == null) {
            throw new AIConversationException(ErrorCode.CONVERSATION_ID_REQUIRED);
        }

        if (conversationId <= 0) {
            throw new AIConversationException(ErrorCode.CONVERSATION_ID_INVALID);
        }
    }

    public void validateRecentMessagesLimit(int recentMessagesLimit) {

        if (recentMessagesLimit < MIN_RECENT_MESSAGES_LIMIT
                || recentMessagesLimit > MAX_RECENT_MESSAGES_LIMIT) {
            throw new AIConversationException(ErrorCode.RECENT_MESSAGES_LIMIT_INVALID);
        }
    }

    // Field-level checks (documentVersionIds, content) are already enforced by the
    // reused createConversation/attachDocuments/sendMessage calls downstream — this
    // is just the top-level null-request guard, consistent with the other public methods.
    public void validateStartDocumentQaRequest(StartDocumentQaConversationRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.REQUEST_REQUIRED);
        }
    }

    // Field-level checks (documentVersionIds, inputData) are already enforced by the
    // reused createConversation/attachDocuments/generate calls downstream — this
    // is just the top-level null-request guard, consistent with the other public methods.
    public void validateStartGenerationRequest(StartGenerationConversationRequest request) {
        if (request == null || request.getConversationType() == null || request.getInputData() == null) {
            throw new BusinessException(ErrorCode.REQUEST_REQUIRED);
        }
    }

    public void validateGenerationConversationType(ConversationType conversationType) {

        if (!GENERATION_CONVERSATION_TYPES.contains(conversationType)) {
            throw new AIConversationException(ErrorCode.CONVERSATION_TYPE_NOT_GENERATION);
        }
    }
}
