package com.enterprise.aiassistant.backend.ai.usage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIUsageSummaryResponse {

    private long todayRequest;
    private long todayToken;
    private BigDecimal todayCost;
    private double todaySuccessRate;

    private long last7DayRequests;
    private long last7DayTokens;
    private BigDecimal last7DayCost;
    private double last7DaySuccessRate;
}