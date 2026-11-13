package com.asset.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 资产分配记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_allocation")
public class AssetAllocation extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分配单号 */
    private String allocationNo;

    /** 资产 ID */
    private Long assetId;

    /** 原保管人 ID（可空） */
    private Long fromUserId;

    /** 新保管人 ID */
    private Long toUserId;

    /** 分配方式：MANUAL 手动 / AUTO 自动 */
    private String allocationType;

    /** 状态：PENDING 待领用 / IN_USE 已领用 */
    private String status;

    /** 备注 */
    private String remark;

    /** 分配时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime allocateTime;

    // ---- 以下为非表字段，用于列表展示 ----
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String assetName;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String assetCode;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String toUserName;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String fromUserName;
}
