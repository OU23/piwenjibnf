package com.asset.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 归还审核参数
 */
@Data
public class ReturnAuditDTO {

    @NotNull(message = "归还单 ID 不能为空")
    private Long returnId;

    /** 1 通过 / 0 驳回 */
    @NotNull(message = "审核结果不能为空")
    private Integer result;

    private String comment;
}
