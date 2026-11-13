package com.asset.management.controller;

import com.asset.management.common.annotation.OperationLog;
import com.asset.management.common.constant.Constants;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.Result;
import com.asset.management.dto.AssetDTO;
import com.asset.management.dto.AssetQuery;
import com.asset.management.entity.Asset;
import com.asset.management.security.SecurityUtil;
import com.asset.management.service.AssetService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资产管理接口
 */
@RestController
@RequestMapping("/api/asset")
public class AssetController {

    @Resource
    private AssetService assetService;

    @PreAuthorize("hasAuthority('asset:list')")
    @GetMapping("/page")
    public Result<PageResult<Asset>> page(AssetQuery query) {
        return Result.success(assetService.page(query));
    }

    @PreAuthorize("hasAuthority('asset:list')")
    @GetMapping("/{id}")
    public Result<Asset> getById(@PathVariable Long id) {
        return Result.success(assetService.getById(id));
    }

    @OperationLog(module = "资产管理", type = Constants.OPERATION_INSERT)
    @PreAuthorize("hasAuthority('asset:add')")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody AssetDTO dto) {
        assetService.add(dto, SecurityUtil.getUserId());
        return Result.success();
    }

    @OperationLog(module = "资产管理", type = Constants.OPERATION_UPDATE)
    @PreAuthorize("hasAuthority('asset:edit')")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody AssetDTO dto) {
        assetService.update(dto);
        return Result.success();
    }

    @OperationLog(module = "资产管理", type = Constants.OPERATION_DELETE)
    @PreAuthorize("hasAuthority('asset:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return Result.success();
    }

    @OperationLog(module = "资产管理", type = "报废")
    @PreAuthorize("hasAuthority('asset:scrap')")
    @PutMapping("/{id}/scrap")
    public Result<Void> scrap(@PathVariable Long id) {
        assetService.scrap(id);
        return Result.success();
    }

    @OperationLog(module = "资产管理", type = "报修")
    @PreAuthorize("hasAuthority('asset:repair')")
    @PutMapping("/{id}/repair")
    public Result<Void> repair(@PathVariable Long id) {
        assetService.repair(id);
        return Result.success();
    }

    @OperationLog(module = "资产管理", type = "维修完成")
    @PreAuthorize("hasAuthority('asset:repair')")
    @PutMapping("/{id}/repairDone")
    public Result<Void> repairDone(@PathVariable Long id) {
        assetService.repairDone(id);
        return Result.success();
    }
}
