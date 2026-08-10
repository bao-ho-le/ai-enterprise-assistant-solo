package com.enterprise.aiassistant.backend.ai.usage.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

// One row of the GROUP BY CAST(created_at AS date) native query in AIUsageLogRepository.
public interface AIUsageDailyProjection {
    LocalDate getDay();

    Long getRequestCount();

    Long getInputTokens();

    Long getOutputTokens();

    Long getTotalTokens();

    BigDecimal getCost();

    Long getSuccessCount();

    Long getFailedCount();
}
