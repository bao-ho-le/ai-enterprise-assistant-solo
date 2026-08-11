package com.enterprise.aiassistant.backend.admin.dashboard.controller;

import com.enterprise.aiassistant.backend.admin.dashboard.dto.AdminDashboardResponse;
import com.enterprise.aiassistant.backend.admin.dashboard.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping
    public AdminDashboardResponse getDashboard() {
        return adminDashboardService.getDashboard();
    }
}
