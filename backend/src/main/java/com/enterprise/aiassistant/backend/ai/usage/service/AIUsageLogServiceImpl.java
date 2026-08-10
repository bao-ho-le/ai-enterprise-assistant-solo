package com.enterprise.aiassistant.backend.ai.usage.service;

import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageDailyResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageSummaryResponse;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.event.AIUsageLogEvent;
import com.enterprise.aiassistant.backend.ai.usage.helper.AIUsageHelper;
import com.enterprise.aiassistant.backend.ai.usage.mapper.AIUsageLogMapper;
import com.enterprise.aiassistant.backend.ai.usage.repository.AIUsageDailyProjection;
import com.enterprise.aiassistant.backend.ai.usage.repository.AIUsageLogRepository;
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
    private final ApplicationEventPublisher applicationEventPublisher;


    // Don't persist immediately. The referenced conversation/message/generation is usually
    // created in the same transaction and isn't committed yet, so inserting the usage log
    // now (even via REQUIRES_NEW) may violate FK constraints. Publishing an event lets the
    // log be persisted only after the outer transaction successfully commits.
    @Override
    public void logAiUsage(AIUsageLogRequest request) {
        aiUsageHelper.validateLogRequest(request);

        applicationEventPublisher.publishEvent(new AIUsageLogEvent(request));
    }

    // The outer transaction has already committed by this point, so the thread's transaction
    // resources haven't been unbound yet — without REQUIRES_NEW, save() below would silently
    // "join" the just-completed transaction instead of opening a new one, and the insert would
    // never actually flush to the database (no error, no SQL logged, id stays null).
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAIUsageLogEvent(AIUsageLogEvent event) {
        AIUsageLog entity = aiUsageLogMapper.toEntity(event.request());

        aiUsageLogRepository.save(entity);
    }

    @Override
    public Page<AIUsageLogResponse> getUsageLogs(AIUsageLogFilterRequest filter, Pageable pageable) {
        aiUsageHelper.validateFilter(filter);

        return aiUsageLogRepository.filterUsageLogs(filter, pageable)

                .map(aiUsageLogMapper::toResponse);
    }

    @Override
    public AIUsageSummaryResponse getSummary() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfLast7Days = LocalDate.now().minusDays(6).atStartOfDay();


        List<AIUsageLog> todayLogs = aiUsageLogRepository.filterUsageLogs(aiUsageHelper.fromDateFilter(startOfToday));
        List<AIUsageLog> last7DayLogs = aiUsageLogRepository.filterUsageLogs(aiUsageHelper.fromDateFilter(startOfLast7Days));


        return aiUsageLogMapper.toSummaryResponse(todayLogs, last7DayLogs);
    }

    @Override
    public List<AIUsageDailyResponse> getDailyUsage(int days) {
        aiUsageHelper.validateDays(days);

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);

        Map<LocalDate, AIUsageDailyProjection> byDay = aiUsageLogRepository
                .findDailyStats(start.atStartOfDay())
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
        return aiUsageLogRepository.findDistinctModels();
    }
}
