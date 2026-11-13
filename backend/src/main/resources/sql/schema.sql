-- ============================================================
-- 企业资产管理系统 数据库初始化脚本
-- 数据库：MySQL 8.x  字符集：utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `asset_management` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `asset_management`;

-- ------------------------------------------------------------
-- 系统用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(64)  NOT NULL COMMENT '登录用户名',
    `password`    VARCHAR(128) NOT NULL COMMENT '密码（BCrypt）',
    `real_name`   VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
    `email`       VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `phone`       VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    `department`  VARCHAR(64)  DEFAULT NULL COMMENT '部门',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ------------------------------------------------------------
-- 系统角色表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name`   VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `role_code`   VARCHAR(64)  NOT NULL COMMENT '角色编码',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- ------------------------------------------------------------
-- 系统菜单表（目录/菜单/按钮 + 权限标识）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    `menu_name`   VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    `menu_type`   CHAR(1)      NOT NULL COMMENT '类型：M目录 C菜单 F按钮',
    `path`        VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
    `component`   VARCHAR(200) DEFAULT NULL COMMENT '前端组件路径',
    `perms`       VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
    `icon`        VARCHAR(64)  DEFAULT NULL COMMENT '图标',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `visible`     TINYINT      NOT NULL DEFAULT 1 COMMENT '可见性：0隐藏 1显示',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';

-- ------------------------------------------------------------
-- 用户-角色关联表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ------------------------------------------------------------
-- 角色-菜单关联表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT,
    `role_id` BIGINT NOT NULL,
    `menu_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ------------------------------------------------------------
-- 部门表（多级树形）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父部门ID，顶级为0',
    `dept_name`   VARCHAR(64)  NOT NULL COMMENT '部门名称',
    `leader`      VARCHAR(64)  DEFAULT NULL COMMENT '负责人',
    `phone`       VARCHAR(32)  DEFAULT NULL COMMENT '联系电话',
    `email`       VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_dept_name` (`dept_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ------------------------------------------------------------
-- 资产分类表（多级树形）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `asset_category`;
CREATE TABLE `asset_category` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `parent_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '父分类ID',
    `category_name` VARCHAR(64)  NOT NULL COMMENT '分类名称',
    `category_code` VARCHAR(64)  NOT NULL COMMENT '分类编码',
    `level`         INT          NOT NULL DEFAULT 1 COMMENT '层级',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `description`   VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `attributes`    TEXT         COMMENT '分类属性配置（JSON数组）',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_code` (`category_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='资产分类表';

-- ------------------------------------------------------------
-- 资产表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `asset`;
CREATE TABLE `asset` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '资产ID',
    `asset_code`    VARCHAR(64)    NOT NULL COMMENT '资产编号',
    `asset_name`    VARCHAR(128)   NOT NULL COMMENT '资产名称',
    `category_id`   BIGINT         NOT NULL COMMENT '分类ID',
    `brand`         VARCHAR(64)    DEFAULT NULL COMMENT '品牌',
    `model`         VARCHAR(64)    DEFAULT NULL COMMENT '型号',
    `serial_number` VARCHAR(128)   DEFAULT NULL COMMENT '序列号',
    `status`        VARCHAR(32)    NOT NULL DEFAULT 'IN_STOCK' COMMENT '状态',
    `price`         DECIMAL(12,2)  DEFAULT NULL COMMENT '购置价格',
    `purchase_date` DATE           DEFAULT NULL COMMENT '购置日期',
    `custodian_id`  BIGINT         DEFAULT NULL COMMENT '保管人ID',
    `location`      VARCHAR(128)   DEFAULT NULL COMMENT '存放位置',
    `remark`        VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_by`     BIGINT         DEFAULT NULL COMMENT '登记人',
    `version`       INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_asset_code` (`asset_code`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='资产表';

-- ------------------------------------------------------------
-- 资产申请单表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `asset_application`;
CREATE TABLE `asset_application` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '申请单ID',
    `application_no` VARCHAR(64)  NOT NULL COMMENT '申请单号',
    `applicant_id`   BIGINT       NOT NULL COMMENT '申请人ID',
    `category_id`    BIGINT       NOT NULL COMMENT '申请分类ID',
    `asset_id`       BIGINT       DEFAULT NULL COMMENT '指定资产ID',
    `quantity`       INT          NOT NULL DEFAULT 1 COMMENT '申请数量',
    `reason`         VARCHAR(500) DEFAULT NULL COMMENT '申请事由',
    `status`         VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    `handle_remark`  VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    `apply_time`     DATETIME     DEFAULT NULL COMMENT '申请时间',
    `handle_time`    DATETIME     DEFAULT NULL COMMENT '处理时间',
    `handler_id`     BIGINT       DEFAULT NULL COMMENT '审批人ID',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_application_no` (`application_no`),
    KEY `idx_applicant` (`applicant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='资产申请单表';

-- ------------------------------------------------------------
-- 审批流水表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `approval_record`;
CREATE TABLE `approval_record` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `business_type` VARCHAR(32)  NOT NULL COMMENT '业务类型：APPLICATION/RETURN',
    `business_id`   BIGINT       NOT NULL COMMENT '业务单ID',
    `approver_id`   BIGINT       NOT NULL COMMENT '审批人ID',
    `result`        TINYINT      NOT NULL COMMENT '结果：1通过 0驳回',
    `comment`       VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    `approve_time`  DATETIME     DEFAULT NULL COMMENT '审批时间',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_business` (`business_type`, `business_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='审批流水表';

-- ------------------------------------------------------------
-- 资产分配记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `asset_allocation`;
CREATE TABLE `asset_allocation` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `allocation_no`   VARCHAR(64)  NOT NULL COMMENT '分配单号',
    `asset_id`        BIGINT       NOT NULL COMMENT '资产ID',
    `from_user_id`    BIGINT       DEFAULT NULL COMMENT '原保管人ID',
    `to_user_id`      BIGINT       NOT NULL COMMENT '新保管人ID',
    `allocation_type` VARCHAR(32)  NOT NULL DEFAULT 'MANUAL' COMMENT '分配方式',
    `status`          VARCHAR(32)  NOT NULL DEFAULT 'ALLOCATED' COMMENT '状态',
    `remark`          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `allocate_time`   DATETIME     DEFAULT NULL COMMENT '分配时间',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_allocation_no` (`allocation_no`),
    KEY `idx_asset_id` (`asset_id`),
    KEY `idx_to_user` (`to_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='资产分配记录表';

-- ------------------------------------------------------------
-- 资产归还单表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `asset_return`;
CREATE TABLE `asset_return` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `return_no`    VARCHAR(64)  NOT NULL COMMENT '归还单号',
    `asset_id`     BIGINT       NOT NULL COMMENT '资产ID',
    `user_id`      BIGINT       NOT NULL COMMENT '归还人ID',
    `reason`       VARCHAR(500) DEFAULT NULL COMMENT '归还原因',
    `status`       VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `return_time`  DATETIME     DEFAULT NULL COMMENT '归还时间',
    `audit_time`   DATETIME     DEFAULT NULL COMMENT '审核时间',
    `auditor_id`   BIGINT       DEFAULT NULL COMMENT '审核人ID',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_return_no` (`return_no`),
    KEY `idx_asset_id` (`asset_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='资产归还单表';

-- ------------------------------------------------------------
-- 操作日志表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT        DEFAULT NULL COMMENT '操作人ID',
    `username`       VARCHAR(64)   DEFAULT NULL COMMENT '操作人用户名',
    `module`         VARCHAR(64)   DEFAULT NULL COMMENT '模块',
    `operation_type` VARCHAR(64)   DEFAULT NULL COMMENT '操作类型',
    `method`         VARCHAR(255)  DEFAULT NULL COMMENT '方法名',
    `request_method` VARCHAR(16)   DEFAULT NULL COMMENT '请求方式',
    `request_url`    VARCHAR(255)  DEFAULT NULL COMMENT '请求地址',
    `params`         TEXT          COMMENT '请求参数',
    `ip`             VARCHAR(64)   DEFAULT NULL COMMENT '操作IP',
    `duration`       BIGINT        DEFAULT NULL COMMENT '耗时(ms)',
    `status`         TINYINT       DEFAULT 1 COMMENT '是否成功：1成功 0失败',
    `error_msg`      VARCHAR(2000) DEFAULT NULL COMMENT '异常信息',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_username` (`username`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
