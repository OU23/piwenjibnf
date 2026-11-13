package com.asset.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@TableName("operation_log")
public class OperationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 ID */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 所属模块 */
    private String module;

    /** 操作类型 */
    private String operationType;

    /** 请求方法名 */
    private String method;

    /** 请求方式 GET/POST 等 */
    private String requestMethod;

    /** 请求地址 */
    private String requestUrl;

    /** 请求参数 */
    private String params;

    /** 操作 IP */
    private String ip;

    /** 耗时（毫秒） */
    private Long duration;

    /** 是否成功：1 成功 / 0 失败 */
    private Integer status;

    /** 异常信息 */
    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
