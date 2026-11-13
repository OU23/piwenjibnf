package com.asset.management.security;

import com.asset.management.common.constant.Constants;
import com.asset.management.common.result.ResultCode;
import com.asset.management.entity.SysMenu;
import com.asset.management.entity.SysRole;
import com.asset.management.entity.SysRoleMenu;
import com.asset.management.entity.SysUser;
import com.asset.management.entity.SysUserRole;
import com.asset.management.mapper.SysMenuMapper;
import com.asset.management.mapper.SysRoleMapper;
import com.asset.management.mapper.SysRoleMenuMapper;
import com.asset.management.mapper.SysUserMapper;
import com.asset.management.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户详情加载：查询用户、角色、权限，供 Spring Security 认证使用
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysRoleMapper sysRoleMapper;
    @Resource
    private SysMenuMapper sysMenuMapper;
    @Resource
    private SysUserRoleMapper sysUserRoleMapper;
    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException(ResultCode.USERNAME_PASSWORD_ERROR.getMessage());
        }

        // 查询角色
        List<SysRole> roles = listRolesByUserId(user.getId());
        List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList());

        // 查询权限
        Set<String> permissions = listPermissions(roles);

        return new LoginUser(user, permissions, roleCodes);
    }

    /**
     * 根据用户 ID 查询角色
     */
    private List<SysRole> listRolesByUserId(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        return sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds).eq(SysRole::getStatus, 1));
    }

    /**
     * 根据角色列表计算权限集合：超级管理员拥有全部权限
     */
    private Set<String> listPermissions(List<SysRole> roles) {
        boolean isSuperAdmin = roles.stream()
                .anyMatch(r -> Constants.SUPER_ADMIN_ROLE.equals(r.getRoleCode()));

        List<SysMenu> menus;
        if (isSuperAdmin) {
            menus = sysMenuMapper.selectList(null);
        } else {
            List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
            if (roleIds.isEmpty()) {
                return Collections.emptySet();
            }
            List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(
                    new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
            if (roleMenus.isEmpty()) {
                return Collections.emptySet();
            }
            List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().collect(Collectors.toList());
            menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, menuIds));
        }

        return menus.stream()
                .map(SysMenu::getPerms)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }
}
