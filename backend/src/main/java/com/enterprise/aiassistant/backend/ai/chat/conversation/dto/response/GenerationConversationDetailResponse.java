package com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GenerationStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.fasterxml.jackson.annotation.JsonRawValue;
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
public class GenerationConversationDetailResponse {

    private Long generationId;

    private Long conversationId;

    private String title;

    private ConversationType conversationType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Timestamps of the latest GenerationRun itself — createdAt/updatedAt above are the
    // conversation's, which don't move when a run is re-triggered.
    private LocalDateTime runCreatedAt;

    private LocalDateTime runUpdatedAt;

    private GenerationStatus status;

    // Raw JSON text, not JsonNode: the app has both Jackson 2 (langchain4j) and Jackson 3
    // (Spring's default) on the classpath, and the entity's JsonNode is Jackson 2 — Spring's
    // Jackson 3 response writer doesn't recognize it as a tree and would serialize it as a bean.
    @JsonRawValue
    private String inputData;

    // Null khi conversationType = EMAIL_GENERATION
    private List<ConversationDocumentResponse> attachedDocuments;

    private Long generatedContentId;

    // True nếu có tài liệu đính kèm đang bị soft-delete — FE dùng để hiển thị cảnh báo
    private boolean hasDeletedAttachedDocuments;
}
