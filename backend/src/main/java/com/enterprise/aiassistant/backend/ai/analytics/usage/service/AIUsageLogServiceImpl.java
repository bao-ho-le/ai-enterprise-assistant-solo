package com.enterprise.aiassistant.backend.ai.analytics.usage.service;

import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.response.AIUsageDailyResponse;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.response.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.response.AIUsageSummaryResponse;
import com.enterprise.aiassistant.backend.ai.analytics.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.analytics.usage.event.AIUsageLogEvent;
import com.enterprise.aiassistant.backend.ai.analytics.usage.helper.AIUsageHelper;
import com.enterprise.aiassistant.backend.ai.analytics.usage.helper.AIUsageScopeHelper;
import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.ai.analytics.usage.mapper.AIUsageLogMapper;
import com.enterprise.aiassistant.backend.ai.analytics.usage.repository.AIUsageDailyProjection;
import com.enterprise.aiassistant.backend.ai.analytics.usage.repository.AIUsageLogRepository;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIUsageLogServiceImpl implements AIUsageLogService {

    private final AIUsageLogMapper aiUsageLogMapper;
    private final AIUsageLogRepository aiUsageLogRepository;
    private final AIUsageHelper aiUsageHelper;
    private final AIUsageScopeHelper aiUsageScopeHelper;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher applicationEventPublisher;


    // Chờ transaction chính commit thành công rồi mới lưu usage log để tránh lỗi FK,
    // vì conversation/message/generation có thể chưa được commit tại thời điểm này.
    @Override
    public void logAiUsage(AIUsageLogRequest request) {
        aiUsageHelper.validateLogRequest(request);

        attachOwner(request);

        applicationEventPublisher.publishEvent(new AIUsageLogEvent(request));
    }


    // Transaction bên ngoài đã commit nhưng resource của thread chưa được giải phóng.
    // REQUIRES_NEW đảm bảo save() tạo transaction mới để dữ liệu thực sự được flush xuống DB.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAIUsageLogEvent(AIUsageLogEvent event) {
        AIUsageLog entity = aiUsageLogMapper.toEntity(event.request());

        aiUsageLogRepository.save(entity);
    }

    @Override
    public Page<AIUsageLogResponse> getUsageLogs(AIUsageLogFilterRequest filter, Pageable pageable) {
        aiUsageHelper.validateFilter(filter);

        aiUsageScopeHelper.applyScope(filter, currentUserService.getCurrentPrincipal());

        return aiUsageLogRepository.filterUsageLogs(filter, pageable)

                .map(aiUsageLogMapper::toResponse);
    }

    @Override
    public AIUsageSummaryResponse getSummary() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfLast7Days = LocalDate.now().minusDays(6).atStartOfDay();


        List<AIUsageLog> todayLogs = aiUsageLogRepository.filterUsageLogs(scopedFromDateFilter(startOfToday));
        List<AIUsageLog> last7DayLogs = aiUsageLogRepository.filterUsageLogs(scopedFromDateFilter(startOfLast7Days));


        return aiUsageLogMapper.toSummaryResponse(todayLogs, last7DayLogs);
    }

    @Override
    public List<AIUsageDailyResponse> getDailyUsage(int days) {
        aiUsageHelper.validateDays(days);

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);

        AIUsageLogFilterRequest scope = scopedFromDateFilter(start.atStartOfDay());

        Map<LocalDate, AIUsageDailyProjection> byDay = aiUsageLogRepository
                .findDailyStats(start.atStartOfDay(), scope.getUserId(), scope.getDepartmentId())
                .stream()
                .collect(Collectors.toMap(AIUsageDailyProjection::getDay, Function.identity()));

        List<AIUsageDailyResponse> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            result.add(aiUsageLogMapper.toDailyResponse(date, byDay.get(date)));
        }
        return result;
    }

    @Override
    public List<String> getDistinctModels() {
        currentUserService.requirePermission(Permission.AI_USAGE_READ_SELF);

        return aiUsageLogRepository.findDistinctModels();
    }




    // Helper

    private void attachOwner(AIUsageLogRequest request) {

        if (request.getUserId() != null) {
            return;
        }

        try {
            UserPrincipal principal = currentUserService.getCurrentPrincipal();
            request.setUserId(principal.getId());
            request.setDepartmentId(principal.getDepartmentId());
        } catch (AuthorizationException ignored) {
            // Tiến trình nền, không có user đăng nhập
        }
    }

    // Tác dụng chính là applyScope - check permission
    private AIUsageLogFilterRequest scopedFromDateFilter(LocalDateTime from) {

        AIUsageLogFilterRequest filter = aiUsageHelper.fromDateFilter(from);

        aiUsageScopeHelper.applyScope(filter, currentUserService.getCurrentPrincipal());

        return filter;
    }

}
