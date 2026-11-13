package com.asset.management.controller;

import com.asset.management.common.annotation.OperationLog;
import com.asset.management.common.constant.Constants;
import com.asset.management.common.exception.BusinessException;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.Result;
import com.asset.management.common.result.ResultCode;
import com.asset.management.dto.AllocationDTO;
import com.asset.management.entity.AssetAllocation;
import com.asset.management.security.SecurityUtil;
import com.asset.management.service.AllocationService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 资产分配接口
 */
@RestController
@RequestMapping("/api/allocation")
public class AllocationController {

    @Resource
    private AllocationService allocationService;

    @OperationLog(module = "资产分配", type = "分配")
    @PreAuthorize("hasAuthority('asset:allocation:allocate')")
    @PostMapping
    public Result<Integer> allocate(@Valid @RequestBody AllocationDTO dto) {
        return Result.success(allocationService.allocate(dto));
    }

    @OperationLog(module = "资产分配", type = "自动分配")
    @PreAuthorize("hasAuthority('asset:allocation:allocate')")
    @PostMapping("/auto")
    public Result<Integer> autoAllocate(@RequestBody Map<String, Object> params) {
        Long categoryId = parseLong(params.get("categoryId"));
        int quantity = parsePositiveInt(params.get("quantity"), 1);
        Long toUserId = parseLong(params.get("toUserId"));
        String remark = params.get("remark") == null ? null : String.valueOf(params.get("remark"));
        return Result.success(allocationService.autoAllocate(categoryId, quantity, toUserId, remark));
    }

    /**
     * 安全解析 Long：null / 空串返回 null，非法格式抛业务异常（而非 500）
     */
    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "参数格式错误");
        }
    }

    /**
     * 安全解析正整数：null / 空串返回默认值，非法格式抛业务异常
     */
    private int parsePositiveInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "参数格式错误");
        }
    }

    @OperationLog(module = "资产分配", type = "领用确认")
    @PreAuthorize("hasAuthority('asset:allocation:confirm')")
    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        allocationService.confirm(id, SecurityUtil.getUserId());
        return Result.success();
    }

    @PreAuthorize("hasAuthority('asset:allocation:list')")
    @GetMapping("/page")
    public Result<PageResult<AssetAllocation>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @RequestParam(required = false) Long assetId,
                                                    @RequestParam(required = false) Long toUserId,
                                                    @RequestParam(required = false) String status) {
        return Result.success(allocationService.page(pageNum, pageSize, assetId, toUserId, status));
    }

    @GetMapping("/my/pending")
    public Result<List<AssetAllocation>> myPending() {
        return Result.success(allocationService.listPendingByUser(SecurityUtil.getUserId()));
    }
}
