package com.asset.management.controller;

import com.asset.management.common.annotation.OperationLog;
import com.asset.management.common.constant.Constants;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.Result;
import com.asset.management.dto.UserDTO;
import com.asset.management.entity.SysUser;
import com.asset.management.service.UserService;
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
 * 用户管理接口
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping("/page")
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String username,
                                            @RequestParam(required = false) String realName,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(userService.page(pageNum, pageSize, username, realName, status));
    }

    @GetMapping("/list")
    public Result<List<SysUser>> listAll() {
        return Result.success(userService.listAll());
    }

    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping("/{id}/roles")
    public Result<List<Long>> listRoleIds(@PathVariable Long id) {
        return Result.success(userService.listRoleIdsByUserId(id));
    }

    @OperationLog(module = "用户管理", type = Constants.OPERATION_INSERT)
    @PreAuthorize("hasAuthority('system:user:add')")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody UserDTO dto) {
        userService.add(dto);
        return Result.success();
    }

    @OperationLog(module = "用户管理", type = Constants.OPERATION_UPDATE)
    @PreAuthorize("hasAuthority('system:user:edit')")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody UserDTO dto) {
        userService.update(dto);
        return Result.success();
    }

    @OperationLog(module = "用户管理", type = Constants.OPERATION_DELETE)
    @PreAuthorize("hasAuthority('system:user:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @OperationLog(module = "用户管理", type = Constants.OPERATION_UPDATE)
    @PreAuthorize("hasAuthority('system:user:edit')")
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.success();
    }
}
