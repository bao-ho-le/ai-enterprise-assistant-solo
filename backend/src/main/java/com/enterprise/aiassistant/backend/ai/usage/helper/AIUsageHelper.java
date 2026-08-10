package com.enterprise.aiassistant.backend.ai.usage.helper;

import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class AIUsageHelper {


    public AIUsageLogFilterRequest fromDateFilter(LocalDateTime from) {
        AIUsageLogFilterRequest filter = new AIUsageLogFilterRequest();
        filter.setFromDate(from);
        return filter;
    }

    public void validateLogRequest(AIUsageLogRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.AI_USAGE_REQUEST_REQUIRED);
        }
        if (request.getConversationType() == null) {
            throw new BusinessException(ErrorCode.AI_USAGE_CONVERSATION_TYPE_REQUIRED);
        }
        if (!StringUtils.hasText(request.getModel())) {
            throw new BusinessException(ErrorCode.AI_USAGE_MODEL_REQUIRED);
        }
        if (request.getStatus() == null) {
            throw new BusinessException(ErrorCode.AI_USAGE_STATUS_REQUIRED);
        }
        if ((request.getInputTokens() != null && request.getInputTokens() < 0)
                || (request.getOutputTokens() != null && request.getOutputTokens() < 0)) {
            throw new BusinessException(ErrorCode.AI_USAGE_INVALID_TOKEN_COUNT);
        }
        if (request.getEstimatedCost() != null && request.getEstimatedCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.AI_USAGE_INVALID_ESTIMATED_COST);
        }
    }

    public static int normalizeToken(Integer token) {
        return token != null ? token : 0;
    }

    public int calculateTotalTokens(Integer inputTokens, Integer outputTokens) {
        return normalizeToken(inputTokens) + normalizeToken(outputTokens);
    }

    public double calculateSuccessRate(long successCount, long totalRequests) {
        return totalRequests == 0
                ? 0
                : (double) successCount / totalRequests * 100;
    }

    public double calculateSuccessRate(List<AIUsageLog> logs) {
        if (logs.isEmpty()) {
            return 0;
        }
        long successCount = logs.stream()
                .filter(l -> l.getStatus() == AIUsageStatus.SUCCESS)
                .count();
        return (double) successCount / logs.size() * 100;
    }

    public void validateFilter(AIUsageLogFilterRequest filter) {
        if (filter == null) {
            throw new BusinessException(ErrorCode.AI_USAGE_REQUEST_REQUIRED);
        }
        if (filter.getFromDate() != null && filter.getToDate() != null
                && filter.getFromDate().isAfter(filter.getToDate())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (filter.getModel() != null && filter.getModel().length() > 100) {
            throw new BusinessException(ErrorCode.AI_USAGE_MODEL_TOO_LONG);
        }
    }

    public void validateDays(int days) {
        if (days < 1 || days > 90) {
            throw new BusinessException(ErrorCode.AI_USAGE_INVALID_DAYS);
        }
    }


    public Specification<AIUsageLog> byFilter(AIUsageLogFilterRequest filter) {
        return (root, query, cb) -> {
            Predicate predicates = cb.conjunction();

            if (filter.getFromDate() != null) {
                predicates = cb.and(predicates,
                        cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates = cb.and(predicates,
                        cb.lessThanOrEqualTo(root.get("createdAt"), filter.getToDate()));
            }

            if (StringUtils.hasText(filter.getModel())) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("model"), filter.getModel()));
            }
            if (filter.getConversationType() != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("conversationType"), filter.getConversationType()));
            }
            if (filter.getStatus() != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("status"), filter.getStatus()));
            }
            return predicates;
        };
    }
}

