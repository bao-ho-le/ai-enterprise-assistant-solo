package com.enterprise.aiassistant.backend.ai.analytics.usage.repository;

import java.math.BigDecimal;

// Dùng chung cho group-by user và group-by department: groupId là userId hoặc departmentId.
public interface AIUsageGroupProjection {

    Long getGroupId();

    Long getRequestCount();

    Long getTotalTokens();

    BigDecimal getCost();
}
