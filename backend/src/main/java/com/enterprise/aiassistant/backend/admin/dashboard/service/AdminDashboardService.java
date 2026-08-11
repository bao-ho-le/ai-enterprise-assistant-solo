package com.enterprise.aiassistant.backend.admin.dashboard.service;

import com.enterprise.aiassistant.backend.admin.dashboard.dto.AdminDashboardResponse;
import com.enterprise.aiassistant.backend.admin.usage.dto.AIUsageOverviewResponse;
import com.enterprise.aiassistant.backend.admin.usage.service.AdminUsageService;
import com.enterprise.aiassistant.backend.department.repository.DepartmentRepository;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;

    private final DepartmentRepository departmentRepository;

    private final DocumentRepository documentRepository;

    private final AdminUsageService adminUsageService;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {

        // Permission check nằm trong getOverview (AI_USAGE_READ_ALL) — chỉ ADMIN đi qua được.
        AIUsageOverviewResponse usage = adminUsageService.getOverview(null, null, null, null);

        return AdminDashboardResponse.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByEnabled(true))
                .totalDepartments(departmentRepository.count())
                .totalDocuments(documentRepository.countByStatus(DocumentStatus.ACTIVE))
                .documentsInTrash(documentRepository.countByStatus(DocumentStatus.DELETED))
                .aiRequestCount(usage.getTotalRequests())
                .aiTotalTokens(usage.getTotalTokens())
                .usageByDepartment(usage.getByDepartment())
                .build();
    }
}
