package com.asset.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset")
public class Asset extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资产编号（唯一） */
    private String assetCode;

    /** 资产名称 */
    private String assetName;

    /** 所属分类 ID */
    private Long categoryId;

    /** 品牌 */
    private String brand;

    /** 型号 */
    private String model;

    /** 序列号 */
    private String serialNumber;

    /** 状态：见 AssetStatusEnum */
    private String status;

    /** 购置价格 */
    private BigDecimal price;

    /** 购置日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    /** 当前保管人/使用人 ID */
    private Long custodianId;

    /** 存放位置 */
    private String location;

    /** 备注 */
    private String remark;

    /** 登记人 */
    private Long createBy;

    /** 乐观锁版本号，用于并发控制 */
    @Version
    private Integer version;

    /** 分类名称（非表字段，用于展示） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String categoryName;

    /** 保管人姓名（非表字段，用于展示） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String custodianName;
}
