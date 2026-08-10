package com.enterprise.aiassistant.backend.ai.usage.service;

import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageDailyResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AIUsageLogService {

    // Gọi từ các module AI khác (write-email, summary, write-report, document-qa)
    // sau mỗi lần gọi model để ghi log — không expose qua controller.
    void logAiUsage(AIUsageLogRequest request);

    Page<AIUsageLogResponse> getUsageLogs(AIUsageLogFilterRequest filter, Pageable pageable);

    AIUsageSummaryResponse getSummary();

    // Zero-filled per-day breakdown for the last `days` calendar days (including today).
    List<AIUsageDailyResponse> getDailyUsage(int days);

    // Distinct model names that have ever been logged — backs the "Model" filter dropdown.
    List<String> getDistinctModels();
}