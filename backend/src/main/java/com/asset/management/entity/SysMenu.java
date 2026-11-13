package com.asset.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统菜单/权限
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单 ID，顶级为 0 */
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 菜单类型：M 目录 / C 菜单 / F 按钮 */
    private String menuType;

    /** 前端路由地址 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 权限标识（如 system:user:list） */
    private String perms;

    /** 菜单图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 是否可见：0 隐藏 / 1 显示 */
    private Integer visible;

    /** 状态：0 禁用 / 1 启用 */
    private Integer status;
}
