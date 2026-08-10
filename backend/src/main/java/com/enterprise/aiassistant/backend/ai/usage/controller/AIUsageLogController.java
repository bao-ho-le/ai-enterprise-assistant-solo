package com.enterprise.aiassistant.backend.ai.usage.controller;

import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageDailyResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageSummaryResponse;
import com.enterprise.aiassistant.backend.ai.usage.service.AIUsageLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/ai-usage")
@RequiredArgsConstructor
public class AIUsageLogController {

    private final AIUsageLogService aiUsageLogService;

    @GetMapping
    public Page<AIUsageLogResponse> getUsageLogs(
            @Valid AIUsageLogFilterRequest filter,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return aiUsageLogService.getUsageLogs(filter, pageable);
    }

    @GetMapping("/summary")
    public AIUsageSummaryResponse getSummary() {
        return aiUsageLogService.getSummary();
    }

    @GetMapping("/daily")
    public List<AIUsageDailyResponse> getDailyUsage(@RequestParam(defaultValue = "7") int days) {
        return aiUsageLogService.getDailyUsage(days);
    }

    @GetMapping("/models")
    public List<String> getModels() {
        return aiUsageLogService.getDistinctModels();
    }
}