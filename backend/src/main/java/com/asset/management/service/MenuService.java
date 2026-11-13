package com.asset.management.service;

import com.asset.management.common.constant.CommonStatus;
import com.asset.management.common.constant.Constants;
import com.asset.management.dto.MenuVO;
import com.asset.management.entity.SysMenu;
import com.asset.management.entity.SysRole;
import com.asset.management.entity.SysRoleMenu;
import com.asset.management.entity.SysUserRole;
import com.asset.management.mapper.SysMenuMapper;
import com.asset.management.mapper.SysRoleMapper;
import com.asset.management.mapper.SysRoleMenuMapper;
import com.asset.management.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单/权限服务：菜单树构建、用户菜单查询、角色菜单分配
 */
@Service
public class MenuService {

    @Resource
    private SysMenuMapper menuMapper;
    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private SysRoleMenuMapper roleMenuMapper;
    @Resource
    private SysUserRoleMapper userRoleMapper;

    /**
     * 判断用户是否超级管理员
     */
    public boolean isSuperAdmin(Long userId) {
        if (Constants.SUPER_ADMIN_ID.equals(userId)) {
            return true;
        }
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return false;
        }
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<SysRole> roles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds));
        return roles.stream().anyMatch(r -> Constants.SUPER_ADMIN_ROLE.equals(r.getRoleCode()));
    }

    /**
     * 查询用户可访问的菜单（超级管理员返回全部）
     */
    public List<SysMenu> listMenusByUserId(Long userId) {
        if (isSuperAdmin(userId)) {
            return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
        }
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        if (roleMenus.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().collect(Collectors.toList());
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .orderByAsc(SysMenu::getSort));
    }

    /**
     * 查询所有菜单（树形，用于角色授权）
     */
    public List<MenuVO> listMenuTree() {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
        // 角色授权树需包含按钮权限，故 includeButton = true
        return buildMenuTree(menus, true);
    }

    /**
     * 构建菜单树（仅保留启用且可见的节点）
     *
     * @param menus         菜单列表
     * @param includeButton 是否包含按钮类型节点（角色授权需要，侧边栏菜单不需要）
     */
    public List<MenuVO> buildMenuTree(List<SysMenu> menus, boolean includeButton) {
        List<SysMenu> filtered = menus.stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == CommonStatus.ENABLED)
                .filter(m -> m.getVisible() == null || m.getVisible() == CommonStatus.VISIBLE)
                .filter(m -> includeButton
                        || Constants.MENU_TYPE_DIR.equals(m.getMenuType())
                        || Constants.MENU_TYPE_MENU.equals(m.getMenuType()))
                .collect(Collectors.toList());
        return buildTree(filtered, Constants.ROOT_PARENT_ID);
    }

    private List<MenuVO> buildTree(List<SysMenu> menus, Long parentId) {
        List<MenuVO> result = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (parentId.equals(menu.getParentId())) {
                MenuVO vo = toVO(menu);
                vo.setChildren(buildTree(menus, menu.getId()));
                result.add(vo);
            }
        }
        return result;
    }

    private MenuVO toVO(SysMenu menu) {
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuType(menu.getMenuType());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setPerms(menu.getPerms());
        vo.setIcon(menu.getIcon());
        vo.setSort(menu.getSort());
        return vo;
    }

    /**
     * 查询角色已分配的菜单 ID 列表
     */
    public List<Long> listMenuIdsByRoleId(Long roleId) {
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        return roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
    }
}
