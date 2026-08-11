package com.enterprise.aiassistant.backend.admin.dashboard.dto;

import com.enterprise.aiassistant.backend.admin.usage.dto.AIUsageGroupResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminDashboardResponse {

    private long totalUsers;

    private long activeUsers;

    private long totalDepartments;

    private long totalDocuments;

    private long documentsInTrash;

    private long aiRequestCount;

    private long aiTotalTokens;

    private List<AIUsageGroupResponse> usageByDepartment;
}
