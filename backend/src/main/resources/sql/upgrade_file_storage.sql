-- ============================================================
-- 增量脚本：文件存储服务（可选 OSS）
-- 适用：已经在使用的 asset_management 库，可重复执行
--
-- 设计要点（借鉴 Network-Drive / y9-storage）：
--   业务表只保存 object_key，不保存真实存储路径，
--   由存储服务按 object_key 定位文件 —— 因此从本地磁盘
--   切换到对象存储时，业务数据完全不需要改动。
--
--   storage_type / storage_channel 记录文件“实际”落在哪个后端，
--   支持新旧文件分散在不同后端（渐进迁移），而非全库一刀切。
-- ============================================================

USE `asset_management`;

CREATE TABLE IF NOT EXISTS `sys_file` (
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `object_key`      varchar(255) NOT NULL                COMMENT '存储对象键，业务表保存这个值',
    `original_name`   varchar(255) NOT NULL                COMMENT '原始文件名',
    `file_suffix`     varchar(20)           DEFAULT NULL   COMMENT '扩展名（小写，不含点）',
    `content_type`    varchar(100)          DEFAULT NULL   COMMENT 'MIME 类型',
    `file_size`       bigint                DEFAULT 0      COMMENT '文件大小（字节）',
    `md5`             char(32)              DEFAULT NULL   COMMENT '内容 MD5，用于秒传与去重',
    `storage_type`    varchar(20)  NOT NULL DEFAULT 'local' COMMENT '实际存储类型：local / oss',
    `storage_channel` varchar(50)           DEFAULT NULL   COMMENT '存储渠道标识，多后端并存与迁移依据',
    `biz_type`        varchar(50)  NOT NULL DEFAULT 'common' COMMENT '业务分类，新增一类工具声明一个即可',
    `biz_id`          bigint                DEFAULT NULL   COMMENT '业务主键，如资产 ID、工具 ID',
    `scope`           varchar(20)  NOT NULL DEFAULT 'public' COMMENT '归属范围：personal / dept / public',
    `dept_name`       varchar(64)           DEFAULT NULL   COMMENT '归属部门',
    `uploader_id`     bigint                DEFAULT NULL   COMMENT '上传人 ID',
    `uploader_name`   varchar(64)           DEFAULT NULL   COMMENT '上传人姓名',
    `ref_count`       int          NOT NULL DEFAULT 1      COMMENT '引用计数，归零才删物理文件',
    `create_time`     datetime              DEFAULT NULL   COMMENT '创建时间',
    `update_time`     datetime              DEFAULT NULL   COMMENT '更新时间',
    `deleted`         tinyint      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_file_object_key` (`object_key`),
    KEY `idx_sys_file_md5` (`md5`),
    KEY `idx_sys_file_biz` (`biz_type`, `biz_id`),
    KEY `idx_sys_file_dept` (`dept_name`),
    KEY `idx_sys_file_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '文件元数据表';
