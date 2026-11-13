package com.asset.management.controller;

import com.asset.management.common.result.Result;
import com.asset.management.service.StatisticsService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 统计分析接口
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(statisticsService.dashboard());
    }

    @PreAuthorize("hasAuthority('statistics:view')")
    @GetMapping("/status")
    public Result<List<Map<String, Object>>> statusDistribution() {
        return Result.success(statisticsService.statusDistribution());
    }

    @PreAuthorize("hasAuthority('statistics:view')")
    @GetMapping("/category")
    public Result<List<Map<String, Object>>> categoryDistribution() {
        return Result.success(statisticsService.categoryDistribution());
    }

    @PreAuthorize("hasAuthority('statistics:view')")
    @GetMapping("/trend")
    public Result<Map<String, Object>> trend() {
        return Result.success(statisticsService.trend());
    }
}
