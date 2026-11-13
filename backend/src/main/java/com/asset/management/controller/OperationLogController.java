package com.asset.management.controller;

import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.Result;
import com.asset.management.entity.OperationLog;
import com.asset.management.service.OperationLogService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志接口
 */
@RestController
@RequestMapping("/api/log")
public class OperationLogController {

    @Resource
    private OperationLogService operationLogService;

    @PreAuthorize("hasAuthority('system:log:list')")
    @GetMapping("/page")
    public Result<PageResult<OperationLog>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize,
                                                 @RequestParam(required = false) String username,
                                                 @RequestParam(required = false) String module) {
        return Result.success(operationLogService.page(pageNum, pageSize, username, module));
    }
}
