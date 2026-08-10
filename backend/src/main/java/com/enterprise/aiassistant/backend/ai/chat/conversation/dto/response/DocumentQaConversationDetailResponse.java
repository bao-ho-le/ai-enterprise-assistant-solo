package com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response;

import com.enterprise.aiassistant.backend.ai.chat.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.AIMessageResponse;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentQaConversationDetailResponse {

    private Long id;

    private String title;

    private ConversationType conversationType;

    private ConversationStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<ConversationDocumentResponse> attachedDocuments;

    // N message gần nhất (mặc định 20, chỉnh bằng query param `recentMessagesLimit`).
    // Lấy đầy đủ + phân trang: gọi GET /ai-conversations/{conversationId}/messages
    private List<AIMessageResponse> recentMessages;

    private boolean hasMoreMessages;

    // True nếu có tài liệu đính kèm đang bị soft-delete — FE dùng để hiển thị cảnh báo
    private boolean hasDeletedAttachedDocuments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachedDocumentItem {
        private Long documentVersionId;
        private Long documentId;
        private String documentTitle;
        private Integer versionNumber;
    }
}
