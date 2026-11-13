package com.asset.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 资产归还单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_return")
public class AssetReturn extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归还单号 */
    private String returnNo;

    /** 资产 ID */
    private Long assetId;

    /** 归还人 ID */
    private Long userId;

    /** 归还原因 */
    private String reason;

    /** 状态：见 ReturnStatusEnum */
    private String status;

    /** 审核意见 */
    private String auditRemark;

    /** 归还时间（提交时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime returnTime;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    /** 审核人 ID */
    private Long auditorId;

    // ---- 以下为非表字段，用于列表展示 ----
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String assetName;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String assetCode;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String userName;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String auditorName;
}
