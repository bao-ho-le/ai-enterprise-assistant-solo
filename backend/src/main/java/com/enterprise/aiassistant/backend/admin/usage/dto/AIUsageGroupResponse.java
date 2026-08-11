package com.enterprise.aiassistant.backend.admin.usage.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AIUsageGroupResponse {

    // userId hoặc departmentId tuỳ nhóm thống kê.
    private Long id;

    private String name;

    private long requestCount;

    private long totalTokens;

    private BigDecimal cost;
}
