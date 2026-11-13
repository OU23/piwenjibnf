package com.asset.management.service;

import com.asset.management.common.result.ResultCode;
import com.asset.management.common.exception.BusinessException;
import com.asset.management.dto.LoginDTO;
import com.asset.management.dto.LoginVO;
import com.asset.management.entity.SysUser;
import com.asset.management.security.JwtUtil;
import com.asset.management.security.LoginUser;
import com.asset.management.security.SecurityUtil;
import jakarta.annotation.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证服务：登录、获取当前用户信息
 */
@Service
public class AuthService {

    @Resource
    private AuthenticationManager authenticationManager;
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private MenuService menuService;

    public LoginVO login(LoginDTO dto) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BusinessException(ResultCode.USERNAME_PASSWORD_ERROR);
        } catch (DisabledException e) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return buildLoginVO(loginUser, true);
    }

    /**
     * 刷新当前登录用户信息（含菜单权限）
     */
    public LoginVO getInfo() {
        return buildLoginVO(SecurityUtil.getLoginUser(), false);
    }

    private LoginVO buildLoginVO(LoginUser loginUser, boolean withToken) {
        SysUser user = loginUser.getUser();
        LoginVO vo = new LoginVO();
        if (withToken) {
            vo.setToken(jwtUtil.generateToken(user.getId(), user.getUsername()));
        }
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setDepartment(user.getDepartment());
        vo.setRoles(List.copyOf(loginUser.getRoles()));
        vo.setPermissions(loginUser.getPermissions());
        vo.setMenus(menuService.buildMenuTree(menuService.listMenusByUserId(user.getId()), false));
        return vo;
    }
}
