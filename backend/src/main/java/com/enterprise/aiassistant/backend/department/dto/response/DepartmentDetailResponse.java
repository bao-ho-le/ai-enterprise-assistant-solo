package com.enterprise.aiassistant.backend.department.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DepartmentDetailResponse {

    private DepartmentResponse department;

    private DepartmentMemberResponse manager;

    private List<DepartmentMemberResponse> members;

    private long documentCount;

    private long aiRequestCount;

    private long aiTotalTokens;

    private BigDecimal aiEstimatedCost;
}
