package com.enterprise.aiassistant.backend.ai.analytics.usage.dto.response;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIUsageLogResponse {

    private Long userId;
    private Long departmentId;
    private LocalDateTime createdAt;
    private ConversationType conversationType;
    private String model;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private BigDecimal estimatedCost;
    private AIUsageStatus status;
}