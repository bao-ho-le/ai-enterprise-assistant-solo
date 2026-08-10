package com.enterprise.aiassistant.backend.ai.analytics.usage.repository;

import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.entity.AIUsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AIUsageLogRepositoryCustom {

    Page<AIUsageLog> filterUsageLogs(AIUsageLogFilterRequest filter, Pageable pageable);

    List<AIUsageLog> filterUsageLogs(AIUsageLogFilterRequest filter);
}
