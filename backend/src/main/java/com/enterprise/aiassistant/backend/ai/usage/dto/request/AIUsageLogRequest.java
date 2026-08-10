package com.enterprise.aiassistant.backend.ai.usage.dto.request;

import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// DTO dùng nội bộ, các module AI khác (write-email, summary, write-report, document-qa)
// build request này sau mỗi lần gọi model rồi truyền vào AIUsageLogService.log(...).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIUsageLogRequest {

    // Optional: only set by callers running inside an AIConversation (generation/QA flows).
    private Long conversationId;

    private Long messageId;

    // Optional: set by generation flows.
    private Long generationId;

    private ConversationType conversationType;
    private String model;
    private Integer inputTokens;
    private Integer outputTokens;
    private BigDecimal estimatedCost;
    private AIUsageStatus status;
    private String errorMessage;
}