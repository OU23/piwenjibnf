package com.asset.management.common.constant;

/**
 * 系统通用常量
 */
public final class Constants {

    /** 超级管理员角色标识 */
    public static final String SUPER_ADMIN_ROLE = "admin";
    /** 超级管理员用户 ID */
    public static final Long SUPER_ADMIN_ID = 1L;

    /** 顶级父节点 ID */
    public static final Long ROOT_PARENT_ID = 0L;

    /** 菜单类型 */
    public static final String MENU_TYPE_DIR = "M";
    public static final String MENU_TYPE_MENU = "C";
    public static final String MENU_TYPE_BUTTON = "F";

    /** 操作日志操作类型 */
    public static final String OPERATION_INSERT = "新增";
    public static final String OPERATION_UPDATE = "修改";
    public static final String OPERATION_DELETE = "删除";
    public static final String OPERATION_QUERY = "查询";
    public static final String OPERATION_OTHER = "其他";

    private Constants() {
    }
}
