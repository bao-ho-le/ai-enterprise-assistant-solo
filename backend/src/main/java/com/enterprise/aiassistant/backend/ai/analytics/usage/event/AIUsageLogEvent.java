package com.enterprise.aiassistant.backend.ai.analytics.usage.event;

import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;

public record AIUsageLogEvent(AIUsageLogRequest request) {
}
