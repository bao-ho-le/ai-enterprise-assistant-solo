package com.enterprise.aiassistant.backend.ai.analytics.usage.repository;

import com.enterprise.aiassistant.backend.ai.analytics.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;


import java.time.LocalDateTime;
import java.util.List;

public interface AIUsageLogRepository

        extends JpaRepository<AIUsageLog, Long>, AIUsageLogRepositoryCustom {


    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    long countByStatus(AIUsageStatus status);


    // One row per calendar day (Postgres date cast), oldest first.
    @Query(value = """
            SELECT CAST(created_at AS date)                          AS day,
                   COUNT(*)                                           AS requestCount,
                   COALESCE(SUM(input_tokens), 0)                     AS inputTokens,
                   COALESCE(SUM(output_tokens), 0)                    AS outputTokens,
                   COALESCE(SUM(total_tokens), 0)                     AS totalTokens,
                   COALESCE(SUM(estimated_cost), 0)                   AS cost,
                   COUNT(*) FILTER (WHERE status = 'SUCCESS')         AS successCount,
                   COUNT(*) FILTER (WHERE status = 'FAILED')          AS failedCount
            FROM ai_usage_logs
            WHERE created_at >= :from
              AND (CAST(:userId AS bigint) IS NULL OR user_id = CAST(:userId AS bigint))
              AND (CAST(:departmentId AS bigint) IS NULL OR department_id = CAST(:departmentId AS bigint))
            GROUP BY CAST(created_at AS date)
            ORDER BY day
            """, nativeQuery = true)
    List<AIUsageDailyProjection> findDailyStats(
            @Param("from") LocalDateTime from,
            @Param("userId") Long userId,
            @Param("departmentId") Long departmentId
    );

    @Query("SELECT DISTINCT a.model FROM AIUsageLog a ORDER BY a.model")
    List<String> findDistinctModels();

    // === Dùng cho GET /ai-conversations/{id} — tổng hợp token/cost theo conversation ===
    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM AIUsageLog u WHERE u.aiConversationId = :conversationId")
    Long sumTotalTokensByConversationId(@Param("conversationId") Long conversationId);

    @Query("SELECT COALESCE(SUM(u.estimatedCost), 0) FROM AIUsageLog u WHERE u.aiConversationId = :conversationId")
    BigDecimal sumEstimatedCostByConversationId(@Param("conversationId") Long conversationId);

    void deleteByAiConversationId(Long aiConversationId);

    // ===================== Admin aggregates =====================

    // nativeQuery + CAST: JPQL "(:param IS NULL OR ...)" để param trần không có ngữ cảnh
    // kiểu dữ liệu nào khác thì Postgres không suy được type ("could not determine data
    // type of parameter $1"). Cùng cách khắc phục đã dùng ở findDailyStats.
    @Query(value = """
            SELECT user_id                                        AS groupId,
                   COUNT(*)                                        AS requestCount,
                   COALESCE(SUM(total_tokens), 0)                  AS totalTokens,
                   COALESCE(SUM(estimated_cost), 0)                AS cost
            FROM ai_usage_logs
            WHERE user_id IS NOT NULL
              AND (CAST(:fromDate AS timestamp) IS NULL OR created_at >= CAST(:fromDate AS timestamp))
              AND (CAST(:toDate AS timestamp) IS NULL OR created_at <= CAST(:toDate AS timestamp))
              AND (CAST(:departmentId AS bigint) IS NULL OR department_id = CAST(:departmentId AS bigint))
              AND (CAST(:userId AS bigint) IS NULL OR user_id = CAST(:userId AS bigint))
            GROUP BY user_id
            ORDER BY COALESCE(SUM(total_tokens), 0) DESC
            """, nativeQuery = true)
    List<AIUsageGroupProjection> aggregateByUser(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("departmentId") Long departmentId,
            @Param("userId") Long userId
    );

    @Query(value = """
            SELECT department_id                                  AS groupId,
                   COUNT(*)                                        AS requestCount,
                   COALESCE(SUM(total_tokens), 0)                  AS totalTokens,
                   COALESCE(SUM(estimated_cost), 0)                AS cost
            FROM ai_usage_logs
            WHERE department_id IS NOT NULL
              AND (CAST(:fromDate AS timestamp) IS NULL OR created_at >= CAST(:fromDate AS timestamp))
              AND (CAST(:toDate AS timestamp) IS NULL OR created_at <= CAST(:toDate AS timestamp))
              AND (CAST(:departmentId AS bigint) IS NULL OR department_id = CAST(:departmentId AS bigint))
              AND (CAST(:userId AS bigint) IS NULL OR user_id = CAST(:userId AS bigint))
            GROUP BY department_id
            ORDER BY COALESCE(SUM(total_tokens), 0) DESC
            """, nativeQuery = true)
    List<AIUsageGroupProjection> aggregateByDepartment(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("departmentId") Long departmentId,
            @Param("userId") Long userId
    );

    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM AIUsageLog u")
    long sumAllTotalTokens();

    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM AIUsageLog u WHERE u.departmentId = :departmentId")
    long sumTotalTokensByDepartmentId(@Param("departmentId") Long departmentId);

    long countByDepartmentId(Long departmentId);

    @Query("SELECT COALESCE(SUM(u.estimatedCost), 0) FROM AIUsageLog u WHERE u.departmentId = :departmentId")
    BigDecimal sumEstimatedCostByDepartmentId(@Param("departmentId") Long departmentId);
}
