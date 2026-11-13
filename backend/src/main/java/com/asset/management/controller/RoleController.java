package com.asset.management.controller;

import com.asset.management.common.annotation.OperationLog;
import com.asset.management.common.constant.Constants;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.Result;
import com.asset.management.dto.RoleDTO;
import com.asset.management.entity.SysRole;
import com.asset.management.service.RoleService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理接口
 */
@RestController
@RequestMapping("/api/role")
public class RoleController {

    @Resource
    private RoleService roleService;

    @PreAuthorize("hasAuthority('system:role:list')")
    @GetMapping("/page")
    public Result<PageResult<SysRole>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String roleName) {
        return Result.success(roleService.page(pageNum, pageSize, roleName));
    }

    @GetMapping("/list")
    public Result<List<SysRole>> listAll() {
        return Result.success(roleService.listAll());
    }

    @PreAuthorize("hasAuthority('system:role:list')")
    @GetMapping("/{id}/menus")
    public Result<List<Long>> listMenuIds(@PathVariable Long id) {
        return Result.success(roleService.listMenuIdsByRoleId(id));
    }

    @OperationLog(module = "角色管理", type = Constants.OPERATION_INSERT)
    @PreAuthorize("hasAuthority('system:role:add')")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody RoleDTO dto) {
        roleService.add(dto);
        return Result.success();
    }

    @OperationLog(module = "角色管理", type = Constants.OPERATION_UPDATE)
    @PreAuthorize("hasAuthority('system:role:edit')")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody RoleDTO dto) {
        roleService.update(dto);
        return Result.success();
    }

    @OperationLog(module = "角色管理", type = "分配权限")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return Result.success();
    }

    @OperationLog(module = "角色管理", type = Constants.OPERATION_DELETE)
    @PreAuthorize("hasAuthority('system:role:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }
}
