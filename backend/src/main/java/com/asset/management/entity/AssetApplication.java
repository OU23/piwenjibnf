package com.asset.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 资产申请单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_application")
public class AssetApplication extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请单号 */
    private String applicationNo;

    /** 申请人 ID */
    private Long applicantId;

    /** 申请分类 ID */
    private Long categoryId;

    /** 指定资产 ID（可选，指定具体资产时不为空） */
    private Long assetId;

    /** 申请数量（按分类申请时使用） */
    private Integer quantity;

    /** 申请事由 */
    private String reason;

    /** 状态：见 ApplicationStatusEnum */
    private String status;

    /** 审批意见 */
    private String handleRemark;

    /** 申请时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;

    /** 处理人（审批人）ID */
    private Long handlerId;

    // ---- 以下为非表字段，用于列表展示 ----
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String applicantName;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String categoryName;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String assetName;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String assetCode;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String handlerName;
}
