package com.enterprise.aiassistant.backend.admin.usage.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class AIUsageOverviewResponse {

    private long totalRequests;

    private long totalTokens;

    private BigDecimal totalCost;

    private List<AIUsageGroupResponse> byUser;

    private List<AIUsageGroupResponse> byDepartment;
}
