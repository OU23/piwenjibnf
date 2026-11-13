package com.asset.management.dto;

import lombok.Data;

/**
 * 文件上传参数。
 *
 * <p>{@code bizType} 是扩展的关键：以后每新增一类工具（如巡检、耗材、合同），
 * 只需要约定一个新的 bizType，不需要改存储层代码。</p>
 */
@Data
public class FileUploadRequest {

    /** 业务分类，如 asset / tool / contract，默认 common */
    private String bizType = "common";

    /** 业务主键，如资产 ID、工具 ID，可为空（先传文件后关联） */
    private Long bizId;

    /** 归属范围：personal 个人 / dept 部门 / public 公共，默认 public */
    private String scope = "public";
}
