package com.enterprise.aiassistant.backend.ai.usage.repository;

import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AIUsageLogRepositoryCustomImpl implements AIUsageLogRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<AIUsageLog> filterUsageLogs(AIUsageLogFilterRequest filter, Pageable pageable) {

        StringBuilder jpql = new StringBuilder("""
                
                SELECT u
                
                FROM AIUsageLog u
                
                WHERE 1=1
                
                """);

        Map<String, Object> params = new HashMap<>();

        appendFilters(jpql, params, filter);

        jpql.append(" ORDER BY u.createdAt DESC");

        TypedQuery<AIUsageLog> query = entityManager.createQuery(jpql.toString(), AIUsageLog.class);

        params.forEach(query::setParameter);

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(
                query.getResultList(),
                pageable,
                countUsageLogs(filter)
        );
    }

    @Override
    public List<AIUsageLog> filterUsageLogs(AIUsageLogFilterRequest filter) {

        StringBuilder jpql = new StringBuilder("""
                
                SELECT u
                
                FROM AIUsageLog u
                
                WHERE 1=1
                
                """);

        Map<String, Object> params = new HashMap<>();

        appendFilters(jpql, params, filter);

        jpql.append(" ORDER BY u.createdAt DESC");

        TypedQuery<AIUsageLog> query = entityManager.createQuery(jpql.toString(), AIUsageLog.class);

        params.forEach(query::setParameter);

        return query.getResultList();
    }

    private long countUsageLogs(AIUsageLogFilterRequest filter) {

        StringBuilder jpql = new StringBuilder("""
                
                SELECT COUNT(u)
                
                FROM AIUsageLog u
                
                WHERE 1=1
                
                """);

        Map<String, Object> params = new HashMap<>();
        appendFilters(jpql, params, filter);

        TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class);
        params.forEach(query::setParameter);

        return query.getSingleResult();
    }

    private void appendFilters(
            StringBuilder jpql,
            Map<String, Object> params,
            AIUsageLogFilterRequest filter
    ) {

        if (filter.getFromDate() != null) {
            jpql.append(" AND u.createdAt >= :fromDate");
            params.put("fromDate", filter.getFromDate());
        }

        if (filter.getToDate() != null) {
            jpql.append(" AND u.createdAt <= :toDate");
            params.put("toDate", filter.getToDate());
        }

        if (filter.getConversationType() != null) {
            jpql.append(" AND u.conversationType = :conversationType");
            params.put("conversationType", filter.getConversationType());
        }

        if (StringUtils.hasText(filter.getModel())) {
            jpql.append(" AND u.model = :model");
            params.put("model", filter.getModel());
        }

        if (filter.getStatus() != null) {
            jpql.append(" AND u.status = :status");
            params.put("status", filter.getStatus());
        }
    }
}
