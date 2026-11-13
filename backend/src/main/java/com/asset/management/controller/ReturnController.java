package com.asset.management.controller;

import com.asset.management.common.annotation.OperationLog;
import com.asset.management.common.constant.Constants;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.Result;
import com.asset.management.dto.ReturnAuditDTO;
import com.asset.management.dto.ReturnDTO;
import com.asset.management.entity.ApprovalRecord;
import com.asset.management.entity.AssetReturn;
import com.asset.management.security.SecurityUtil;
import com.asset.management.service.ReturnService;
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
 * 资产归还接口
 */
@RestController
@RequestMapping("/api/return")
public class ReturnController {

    @Resource
    private ReturnService returnService;

    @OperationLog(module = "资产归还", type = Constants.OPERATION_INSERT)
    @PreAuthorize("hasAuthority('asset:return:submit')")
    @PostMapping
    public Result<Void> submit(@Valid @RequestBody ReturnDTO dto) {
        returnService.submit(dto, SecurityUtil.getUserId());
        return Result.success();
    }

    @OperationLog(module = "资产归还", type = "审核")
    @PreAuthorize("hasAuthority('asset:return:audit')")
    @PutMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody ReturnAuditDTO dto) {
        returnService.audit(dto.getReturnId(), dto.getResult(), dto.getComment(), SecurityUtil.getUserId());
        return Result.success();
    }

    @PreAuthorize("hasAuthority('asset:return:list')")
    @GetMapping("/page")
    public Result<PageResult<AssetReturn>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) Long userId) {
        return Result.success(returnService.page(pageNum, pageSize, status, userId));
    }

    @PreAuthorize("hasAuthority('asset:return:list')")
    @GetMapping("/{id}/records")
    public Result<List<ApprovalRecord>> listRecords(@PathVariable Long id) {
        return Result.success(returnService.listApprovalRecords(id));
    }
}
