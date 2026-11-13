package com.asset.management.controller;

import com.asset.management.common.result.Result;
import com.asset.management.dto.MenuVO;
import com.asset.management.service.MenuService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单接口
 */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    @Resource
    private MenuService menuService;

    @PreAuthorize("hasAuthority('system:menu:list')")
    @GetMapping("/tree")
    public Result<List<MenuVO>> tree() {
        return Result.success(menuService.listMenuTree());
    }
}
