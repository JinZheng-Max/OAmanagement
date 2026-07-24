package com.wut.practicum.controller;

import com.wut.practicum.common.ApiResult;
import com.wut.practicum.dto.AdminDashboardVO;
import com.wut.practicum.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "仪表盘数据统计接口")
@RestController
@RequestMapping({"/api/dashboard", "/api/departments/dashboard"})
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "【管理员】获取超级管理员运维数据大屏聚合统计数据")
    @GetMapping("/admin/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResult<AdminDashboardVO> getAdminDashboardStats() {
        return ApiResult.success(dashboardService.getAdminDashboardStats());
    }
}
