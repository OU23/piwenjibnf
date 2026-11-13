package com.asset.management.config;

import com.asset.management.common.constant.CommonStatus;
import com.asset.management.entity.SysRole;
import com.asset.management.entity.SysUser;
import com.asset.management.entity.SysUserRole;
import com.asset.management.mapper.SysRoleMapper;
import com.asset.management.mapper.SysUserMapper;
import com.asset.management.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器：首次启动时自动创建演示用户（幂等）
 * 演示账号：
 *   admin / admin123     超级管理员
 *   manager / 123456     资产管理员
 *   employee / 123456    普通员工
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    @Resource
    private SysUserMapper userMapper;
    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private SysUserRoleMapper userRoleMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        createUserIfAbsent("admin", "admin123", "系统管理员", "信息中心", "admin");
        createUserIfAbsent("manager", "123456", "资产管理专员", "资产管理部", "asset_manager");
        createUserIfAbsent("employee", "123456", "普通员工", "市场部", "employee");
    }

    private void createUserIfAbsent(String username, String rawPassword, String realName,
                                    String department, String roleCode) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (count != null && count > 0) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(realName);
        user.setDepartment(department);
        user.setStatus(CommonStatus.ENABLED);
        user.setRemark("系统自动初始化账号");
        userMapper.insert(user);

        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
        if (role != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }
        log.info("已初始化用户: {} / {} (角色: {})", username, rawPassword, roleCode);
    }
}
