package com.asset.management.controller;

import com.asset.management.common.annotation.OperationLog;
import com.asset.management.common.constant.Constants;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.Result;
import com.asset.management.dto.ApplicationDTO;
import com.asset.management.dto.ApprovalDTO;
import com.asset.management.entity.ApprovalRecord;
import com.asset.management.entity.AssetApplication;
import com.asset.management.security.SecurityUtil;
import com.asset.management.service.ApplicationService;
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

/**
 * 资产申请接口
 */
@RestController
@RequestMapping("/api/application")
public class ApplicationController {

    @Resource
    private ApplicationService applicationService;

    @OperationLog(module = "资产申请", type = Constants.OPERATION_INSERT)
    @PreAuthorize("hasAuthority('asset:application:submit')")
    @PostMapping
    public Result<Void> submit(@Valid @RequestBody ApplicationDTO dto) {
        applicationService.submit(dto, SecurityUtil.getUserId());
        return Result.success();
    }

    @PreAuthorize("hasAuthority('asset:application:list')")
    @GetMapping("/page")
    public Result<PageResult<AssetApplication>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "10") long pageSize,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) Long applicantId,
                                                     @RequestParam(required = false) String keyword) {
        return Result.success(applicationService.page(pageNum, pageSize, status, applicantId, keyword));
    }

    @OperationLog(module = "资产申请", type = "审批")
    @PreAuthorize("hasAuthority('asset:application:approve')")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @Valid @RequestBody ApprovalDTO dto) {
        applicationService.approve(id, dto.getResult(), dto.getComment(), SecurityUtil.getUserId());
        return Result.success();
    }

    @OperationLog(module = "资产申请", type = "撤销")
    @PreAuthorize("hasAuthority('asset:application:submit')")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        applicationService.cancel(id, SecurityUtil.getUserId());
        return Result.success();
    }

    @PreAuthorize("hasAuthority('asset:application:list')")
    @GetMapping("/{id}/records")
    public Result<List<ApprovalRecord>> listRecords(@PathVariable Long id) {
        return Result.success(applicationService.listApprovalRecords(id));
    }
}
