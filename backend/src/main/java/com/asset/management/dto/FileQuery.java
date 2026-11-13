package com.asset.management.dto;

import lombok.Data;

/**
 * 文件分页查询条件。
 *
 * <p>工具（业务分类）变多以后，靠 {@code bizType} + {@code bizId} + {@code deptName}
 * 就能把文件按业务、按部门筛出来，而不用去翻目录。</p>
 */
@Data
public class FileQuery {

    private long pageNum = 1;
    private long pageSize = 10;

    /** 业务分类 */
    private String bizType;

    /** 业务主键 */
    private Long bizId;

    /** 归属范围：personal / dept / public */
    private String scope;

    /** 归属部门 */
    private String deptName;

    /** 上传人 ID */
    private Long uploaderId;

    /** 原始文件名（模糊） */
    private String originalName;

    /** 实际存储类型：local / oss，用于排查迁移进度 */
    private String storageType;
}
