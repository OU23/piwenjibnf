package com.asset.management.service;

import com.asset.management.common.constant.ApplicationStatusEnum;
import com.asset.management.common.constant.AssetStatusEnum;
import com.asset.management.common.constant.ReturnStatusEnum;
import com.asset.management.mapper.StatisticsMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计分析服务：仪表盘概览、状态分布、分类分布、趋势分析
 */
@Service
public class StatisticsService {

    @Resource
    private StatisticsMapper statisticsMapper;

    /**
     * 仪表盘概览数据
     */
    public Map<String, Object> dashboard() {
        List<Map<String, Object>> statusList = statisticsMapper.countByStatus();
        Map<String, Object> result = new HashMap<>();
        result.put("totalAssets", statusList.stream().mapToLong(m -> toLong(m.get("value"))).sum());
        result.put("totalValue", statisticsMapper.sumAssetValue());
        result.put("inStockCount", countStatus(statusList, AssetStatusEnum.IN_STOCK.getCode()));
        result.put("inUseCount", countStatus(statusList, AssetStatusEnum.IN_USE.getCode()));
        result.put("repairingCount", countStatus(statusList, AssetStatusEnum.REPAIRING.getCode()));
        result.put("scrappedCount", countStatus(statusList, AssetStatusEnum.SCRAPPED.getCode()));
        result.put("pendingApplications",
                statisticsMapper.countApplicationByStatus(ApplicationStatusEnum.PENDING.getCode()));
        result.put("pendingReturns",
                statisticsMapper.countReturnByStatus(ReturnStatusEnum.PENDING.getCode()));
        result.put("pendingAllocations",
                statisticsMapper.countAllocationByStatus(AssetStatusEnum.ALLOCATED.getCode()));
        // 首页图表数据
        result.put("statusDistribution", statusDistribution());
        result.put("categoryDistribution", categoryDistribution());
        result.put("trend", trend());
        return result;
    }

    /**
     * 资产状态分布（用于饼图）
     */
    public List<Map<String, Object>> statusDistribution() {
        List<Map<String, Object>> list = statisticsMapper.countByStatus();
        list.forEach(m -> m.put("name", AssetStatusEnum.getDesc(String.valueOf(m.get("name")))));
        return list;
    }

    /**
     * 资产分类分布（用于柱状图）
     */
    public List<Map<String, Object>> categoryDistribution() {
        return statisticsMapper.countByCategory();
    }

    /**
     * 趋势分析（近 6 个月资产入库与分配趋势）
     */
    public Map<String, Object> trend() {
        Map<String, Object> result = new HashMap<>();
        result.put("assetTrend", statisticsMapper.monthlyAssetTrend());
        result.put("allocationTrend", statisticsMapper.monthlyAllocationTrend());
        return result;
    }

    private long countStatus(List<Map<String, Object>> statusList, String code) {
        return statusList.stream()
                .filter(m -> code.equals(String.valueOf(m.get("name"))))
                .mapToLong(m -> toLong(m.get("value")))
                .sum();
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
    }
}
