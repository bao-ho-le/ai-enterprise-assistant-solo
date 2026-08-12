package com.enterprise.aiassistant.backend.admin.usage.controller;

import com.enterprise.aiassistant.backend.admin.usage.dto.AIUsageOverviewResponse;
import com.enterprise.aiassistant.backend.admin.usage.service.AdminUsageService;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.response.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.analytics.usage.service.AIUsageLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("${api.prefix}/admin/ai-usage")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsageController {

    private final AdminUsageService adminUsageService;

    private final AIUsageLogService aiUsageLogService;

    @GetMapping("/overview")
    public AIUsageOverviewResponse getOverview(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,

            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long departmentId
    ) {
        return adminUsageService.getOverview(fromDate, toDate, userId, departmentId);
    }

    // Log chi tiết dùng lại service hiện có — admin có AI_USAGE_READ_ALL nên scope không bị thu hẹp.
    @GetMapping
    public Page<AIUsageLogResponse> getUsageLogs(
            @Valid AIUsageLogFilterRequest filter,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return aiUsageLogService.getUsageLogs(filter, pageable);
    }

    @GetMapping("/models")
    public List<String> getModels() {
        return aiUsageLogService.getDistinctModels();
    }
}
