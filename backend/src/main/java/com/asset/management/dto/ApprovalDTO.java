package com.asset.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批操作参数
 */
@Data
public class ApprovalDTO {

    @NotNull(message = "业务单 ID 不能为空")
    private Long businessId;

    /** 1 通过 / 0 驳回 */
    @NotNull(message = "审批结果不能为空")
    private Integer result;

    /** 审批意见 */
    private String comment;
}
