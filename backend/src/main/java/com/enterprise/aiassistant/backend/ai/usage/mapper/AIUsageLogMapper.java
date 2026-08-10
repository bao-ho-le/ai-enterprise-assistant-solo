package com.enterprise.aiassistant.backend.ai.usage.mapper;

import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageDailyResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageSummaryResponse;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.helper.AIUsageHelper;
import com.enterprise.aiassistant.backend.ai.usage.repository.AIUsageDailyProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AIUsageLogMapper {

    private final AIUsageHelper aiUsageHelper;

    public AIUsageLog toEntity(AIUsageLogRequest request) {

        return AIUsageLog.builder()
                .aiConversationId(request.getConversationId())
                .aiMessageId(request.getMessageId())
                .generationId(request.getGenerationId())
                .conversationType(request.getConversationType())
                .model(request.getModel())
                .inputTokens(request.getInputTokens())
                .outputTokens(request.getOutputTokens())
                .totalTokens(
                        aiUsageHelper.calculateTotalTokens(
                                request.getInputTokens(), request.getOutputTokens()
                        )
                )
                .estimatedCost(request.getEstimatedCost() != null ? request.getEstimatedCost() : BigDecimal.ZERO)
                .status(request.getStatus())
                .errorMessage(request.getErrorMessage())
                .build();
    }

    public AIUsageLogResponse toResponse(AIUsageLog entity) {
        return AIUsageLogResponse.builder()
                .createdAt(entity.getCreatedAt())
                .conversationType(entity.getConversationType())
                .model(entity.getModel())
                .inputTokens(entity.getInputTokens())
                .outputTokens(entity.getOutputTokens())
                .totalTokens(entity.getTotalTokens())
                .estimatedCost(entity.getEstimatedCost())
                .status(entity.getStatus())
                .build();
    }

    public AIUsageSummaryResponse toSummaryResponse(
            List<AIUsageLog> todayLogs,
            List<AIUsageLog> last7DayLogs
    ) {
        return AIUsageSummaryResponse.builder()
                .todayRequest(todayLogs.size())
                .todayToken(sumTokens(todayLogs))
                .todayCost(sumCost(todayLogs))
                .todaySuccessRate(aiUsageHelper.calculateSuccessRate(todayLogs))
                .last7DayRequests(last7DayLogs.size())
                .last7DayTokens(sumTokens(last7DayLogs))
                .last7DayCost(sumCost(last7DayLogs))
                .last7DaySuccessRate(aiUsageHelper.calculateSuccessRate(last7DayLogs))
                .build();
    }

    public AIUsageDailyResponse toDailyResponse(LocalDate date, AIUsageDailyProjection row) {
        if (row == null) {
            return AIUsageDailyResponse.builder()
                    .date(date)
                    .cost(BigDecimal.ZERO)
                    .build();
        }
        return AIUsageDailyResponse.builder()
                .date(date)
                .requestCount(row.getRequestCount())
                .inputTokens(row.getInputTokens())
                .outputTokens(row.getOutputTokens())
                .totalTokens(row.getTotalTokens())
                .cost(row.getCost())
                .successCount(row.getSuccessCount())
                .failedCount(row.getFailedCount())
                .build();
    }

    private long sumTokens(List<AIUsageLog> logs) {
        return logs.stream().mapToLong(AIUsageLog::getTotalTokens).sum();
    }

    private BigDecimal sumCost(List<AIUsageLog> logs) {
        return logs.stream()
                .map(AIUsageLog::getEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
