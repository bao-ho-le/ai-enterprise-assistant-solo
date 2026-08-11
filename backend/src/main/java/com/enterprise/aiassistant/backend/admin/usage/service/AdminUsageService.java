package com.enterprise.aiassistant.backend.admin.usage.service;

import com.enterprise.aiassistant.backend.admin.usage.dto.AIUsageGroupResponse;
import com.enterprise.aiassistant.backend.admin.usage.dto.AIUsageOverviewResponse;
import com.enterprise.aiassistant.backend.ai.analytics.usage.repository.AIUsageGroupProjection;
import com.enterprise.aiassistant.backend.ai.analytics.usage.repository.AIUsageLogRepository;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.department.repository.DepartmentRepository;
import com.enterprise.aiassistant.backend.user.entity.Permission;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Tổng hợp AI usage cho màn Admin. Không tạo hệ thống tracking mới — đọc lại ai_usage_logs hiện có.
@Service
@RequiredArgsConstructor
public class AdminUsageService {

    private final AIUsageLogRepository aiUsageLogRepository;

    private final UserRepository userRepository;

    private final DepartmentRepository departmentRepository;

    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public AIUsageOverviewResponse getOverview(
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Long userId,
            Long departmentId
    ) {

        validateDateRange(fromDate, toDate);

        currentUserService.requirePermission(Permission.AI_USAGE_READ_ALL);

        List<AIUsageGroupProjection> byUser =
                aiUsageLogRepository.aggregateByUser(fromDate, toDate, departmentId, userId);

        List<AIUsageGroupProjection> byDepartment =
                aiUsageLogRepository.aggregateByDepartment(fromDate, toDate, departmentId, userId);

        Map<Long, String> userNames = userRepository
                .findByIdIn(byUser.stream().map(AIUsageGroupProjection::getGroupId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));

        Map<Long, String> departmentNames = departmentRepository
                .findAllById(byDepartment.stream().map(AIUsageGroupProjection::getGroupId).toList())
                .stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));

        return AIUsageOverviewResponse.builder()
                .totalRequests(sumRequests(byUser))
                .totalTokens(sumTokens(byUser))
                .totalCost(sumCost(byUser))
                .byUser(toGroupResponses(byUser, userNames::get))
                .byDepartment(toGroupResponses(byDepartment, departmentNames::get))
                .build();
    }

    // Helper

    private void validateDateRange(LocalDateTime fromDate, LocalDateTime toDate) {

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private List<AIUsageGroupResponse> toGroupResponses(
            List<AIUsageGroupProjection> rows,
            Function<Long, String> nameResolver
    ) {

        return rows.stream()
                .map(row -> AIUsageGroupResponse.builder()
                        .id(row.getGroupId())
                        .name(nameResolver.apply(row.getGroupId()))
                        .requestCount(normalize(row.getRequestCount()))
                        .totalTokens(normalize(row.getTotalTokens()))
                        .cost(row.getCost() != null ? row.getCost() : BigDecimal.ZERO)
                        .build())
                .toList();
    }

    private long sumRequests(List<AIUsageGroupProjection> rows) {
        return rows.stream().mapToLong(row -> normalize(row.getRequestCount())).sum();
    }

    private long sumTokens(List<AIUsageGroupProjection> rows) {
        return rows.stream().mapToLong(row -> normalize(row.getTotalTokens())).sum();
    }

    private BigDecimal sumCost(List<AIUsageGroupProjection> rows) {
        return rows.stream()
                .map(row -> row.getCost() != null ? row.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long normalize(Long value) {
        return value != null ? value : 0L;
    }
}
