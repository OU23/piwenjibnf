package com.asset.management.controller;

import com.asset.management.common.annotation.OperationLog;
import com.asset.management.common.constant.Constants;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.Result;
import com.asset.management.dto.DeptDTO;
import com.asset.management.service.DeptService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 部门管理接口：树形结构 + 分页搜索 + 批量导入导出
 */
@RestController
@RequestMapping("/api/dept")
public class DeptController {

    @Resource
    private DeptService deptService;

    /** 完整部门树（下拉选择、树形表格展示） */
    @GetMapping("/tree")
    public Result<List<DeptDTO>> tree() {
        return Result.success(deptService.listTree());
    }

    /** 分页 + 搜索（部门名称、负责人、联系电话、状态） */
    @GetMapping("/page")
    public Result<PageResult<DeptDTO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) String leader,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status) {
        return Result.success(deptService.page(pageNum, pageSize, deptName, leader, phone, status));
    }

    @OperationLog(module = "部门管理", type = Constants.OPERATION_INSERT)
    @PreAuthorize("hasAuthority('system:dept:add')")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody DeptDTO dto) {
        deptService.add(dto);
        return Result.success();
    }

    @OperationLog(module = "部门管理", type = Constants.OPERATION_UPDATE)
    @PreAuthorize("hasAuthority('system:dept:edit')")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody DeptDTO dto) {
        deptService.update(dto);
        return Result.success();
    }

    @OperationLog(module = "部门管理", type = Constants.OPERATION_DELETE)
    @PreAuthorize("hasAuthority('system:dept:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.success();
    }

    /** 导出部门 CSV（Excel 可直接打开） */
    @OperationLog(module = "部门管理", type = Constants.OPERATION_OTHER)
    @PreAuthorize("hasAuthority('system:dept:export')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        String csv = deptService.exportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dept-list.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    /** 导入部门 CSV（表头：部门名称,上级部门,负责人,联系电话,邮箱,排序,状态,备注） */
    @OperationLog(module = "部门管理", type = Constants.OPERATION_OTHER)
    @PreAuthorize("hasAuthority('system:dept:import')")
    @PostMapping("/import")
    public Result<Map<String, String>> importCsv(@RequestParam("file") MultipartFile file) {
        String message = deptService.importCsv(file);
        return Result.success(Map.of("message", message));
    }
}
