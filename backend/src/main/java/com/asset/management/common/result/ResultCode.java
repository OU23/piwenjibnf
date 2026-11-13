package com.asset.management.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一返回状态码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),

    // 业务错误码
    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_PASSWORD_ERROR(1002, "用户名或密码错误"),
    USER_DISABLED(1003, "账号已被禁用"),
    USERNAME_EXISTS(1004, "用户名已存在"),
    ROLE_CODE_EXISTS(1005, "角色编码已存在"),
    CATEGORY_CODE_EXISTS(1006, "分类编码已存在"),
    CATEGORY_HAS_CHILDREN(1007, "分类下存在子分类，无法删除"),
    CATEGORY_HAS_ASSET(1008, "分类下存在资产，无法删除"),
    ASSET_CODE_EXISTS(1009, "资产编号已存在"),
    ASSET_NOT_FOUND(1010, "资产不存在"),
    ASSET_STATUS_INVALID(1011, "资产状态不允许该操作"),
    APPLICATION_NOT_FOUND(1012, "申请单不存在"),
    APPLICATION_STATUS_INVALID(1013, "申请单状态不允许该操作"),
    ALLOCATION_NOT_FOUND(1014, "分配记录不存在"),
    RETURN_NOT_FOUND(1015, "归还单不存在"),
    RETURN_STATUS_INVALID(1016, "归还单状态不允许该操作"),
    DATA_INCONSISTENT(1017, "数据不一致，请刷新后重试"),
    INSUFFICIENT_STOCK(1018, "可分配资产数量不足"),
    OLD_PASSWORD_ERROR(1019, "原密码错误"),
    DEPT_NOT_FOUND(1020, "部门不存在"),
    DEPT_NAME_EXISTS(1021, "同级下已存在同名部门"),
    DEPT_HAS_CHILDREN(1022, "部门下存在子部门，无法删除"),
    DEPT_HAS_USERS(1023, "部门下存在用户，无法删除"),
    DEPT_CYCLE(1024, "不能将部门移动到自身或其子部门下"),
    DEPT_IMPORT_FILE_EMPTY(1025, "导入文件为空"),

    // 文件存储错误码
    FILE_EMPTY(1026, "上传文件为空"),
    FILE_NOT_FOUND(1027, "文件不存在或已被删除"),
    STORAGE_NOT_CONFIGURED(1028, "文件存储服务未正确配置"),
    STORAGE_ERROR(1029, "文件存储操作失败");

    private final int code;
    private final String message;
}
