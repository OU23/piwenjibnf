package com.asset.management.controller;

import com.asset.management.common.annotation.OperationLog;
import com.asset.management.common.constant.Constants;
import com.asset.management.common.result.Result;
import com.asset.management.dto.CategoryDTO;
import com.asset.management.entity.AssetCategory;
import com.asset.management.service.CategoryService;
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

import java.util.List;

/**
 * 资产分类接口
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @GetMapping("/tree")
    public Result<List<CategoryDTO>> tree() {
        return Result.success(categoryService.listTree());
    }

    @GetMapping("/list")
    public Result<List<AssetCategory>> listAll() {
        return Result.success(categoryService.listAll());
    }

    @OperationLog(module = "资产分类", type = Constants.OPERATION_INSERT)
    @PreAuthorize("hasAuthority('asset:category:add')")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody CategoryDTO dto) {
        categoryService.add(dto);
        return Result.success();
    }

    @OperationLog(module = "资产分类", type = Constants.OPERATION_UPDATE)
    @PreAuthorize("hasAuthority('asset:category:edit')")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody CategoryDTO dto) {
        categoryService.update(dto);
        return Result.success();
    }

    @OperationLog(module = "资产分类", type = Constants.OPERATION_DELETE)
    @PreAuthorize("hasAuthority('asset:category:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
