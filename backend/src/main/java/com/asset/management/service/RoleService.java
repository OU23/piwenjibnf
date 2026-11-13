package com.asset.management.service;

import com.asset.management.common.constant.CommonStatus;
import com.asset.management.common.constant.Constants;
import com.asset.management.common.exception.BusinessException;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.ResultCode;
import com.asset.management.dto.RoleDTO;
import com.asset.management.entity.SysRole;
import com.asset.management.entity.SysRoleMenu;
import com.asset.management.entity.SysUserRole;
import com.asset.management.mapper.SysRoleMapper;
import com.asset.management.mapper.SysRoleMenuMapper;
import com.asset.management.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色服务：角色 CRUD 与角色-菜单分配
 */
@Service
public class RoleService {

    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private SysRoleMenuMapper roleMenuMapper;
    @Resource
    private SysUserRoleMapper userRoleMapper;
    @Resource
    private MenuService menuService;

    public PageResult<SysRole> page(long pageNum, long pageSize, String roleName) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName)
                .orderByAsc(SysRole::getSort);
        Page<SysRole> page = roleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    public List<SysRole> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, CommonStatus.ENABLED)
                .orderByAsc(SysRole::getSort));
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(RoleDTO dto) {
        checkRoleCodeUnique(dto.getRoleCode(), null);
        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus() == null ? CommonStatus.ENABLED : dto.getStatus());
        role.setSort(dto.getSort() == null ? 0 : dto.getSort());
        roleMapper.insert(role);
        saveRoleMenus(role.getId(), dto.getMenuIds());
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(RoleDTO dto) {
        SysRole role = roleMapper.selectById(dto.getId());
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (Constants.SUPER_ADMIN_ROLE.equals(role.getRoleCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "超级管理员角色不允许修改");
        }
        checkRoleCodeUnique(dto.getRoleCode(), dto.getId());
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus());
        role.setSort(dto.getSort());
        roleMapper.updateById(role);
        // 重新分配菜单（仅在显式传入 menuIds 时）
        if (dto.getMenuIds() != null) {
            roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, dto.getId()));
            saveRoleMenus(role.getId(), dto.getMenuIds());
        }
    }

    /**
     * 单独分配角色权限（菜单）
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (Constants.SUPER_ADMIN_ROLE.equals(role.getRoleCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "超级管理员角色不允许修改");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        saveRoleMenus(roleId, menuIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (Constants.SUPER_ADMIN_ROLE.equals(role.getRoleCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "超级管理员角色不允许删除");
        }
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.FAILED.getCode(), "该角色已分配用户，无法删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
    }

    public List<Long> listMenuIdsByRoleId(Long roleId) {
        return menuService.listMenuIdsByRoleId(roleId);
    }

    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        for (Long menuId : menuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenuMapper.insert(roleMenu);
        }
    }

    private void checkRoleCodeUnique(String roleCode, Long excludeId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode);
        if (excludeId != null) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.ROLE_CODE_EXISTS);
        }
    }
}
