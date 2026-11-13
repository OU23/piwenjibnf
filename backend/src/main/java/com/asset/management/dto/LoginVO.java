package com.asset.management.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 登录响应：Token + 用户信息 + 角色 + 权限 + 菜单
 */
@Data
public class LoginVO {

    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String avatar;
    private String department;
    private List<String> roles;
    private Set<String> permissions;
    private List<MenuVO> menus;
}
