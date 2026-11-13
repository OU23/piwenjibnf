package com.asset.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 资产归还提交参数
 */
@Data
public class ReturnDTO {

    @NotNull(message = "资产 ID 不能为空")
    private Long assetId;

    private String reason;
}
