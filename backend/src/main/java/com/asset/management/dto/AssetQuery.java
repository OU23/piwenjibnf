package com.asset.management.dto;

import lombok.Data;

/**
 * 资产分页查询条件
 */
@Data
public class AssetQuery {

    private long pageNum = 1;
    private long pageSize = 10;

    /** 资产名称（模糊） */
    private String assetName;

    /** 资产编号（模糊） */
    private String assetCode;

    /** 分类 ID */
    private Long categoryId;

    /** 状态 */
    private String status;

    /** 保管人 ID */
    private Long custodianId;

    /** 品牌（模糊） */
    private String brand;
}
