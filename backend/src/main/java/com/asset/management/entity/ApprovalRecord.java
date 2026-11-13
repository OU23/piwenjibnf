package com.asset.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审批流水记录（记录每一次审批动作，形成完整审批轨迹）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_record")
public class ApprovalRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型：APPLICATION / RETURN */
    private String businessType;

    /** 业务单 ID */
    private Long businessId;

    /** 审批人 ID */
    private Long approverId;

    /** 审批结果：1 通过 / 0 驳回 */
    private Integer result;

    /** 审批意见 */
    private String comment;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    /** 审批人姓名（非表字段，用于展示） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String approverName;
}
