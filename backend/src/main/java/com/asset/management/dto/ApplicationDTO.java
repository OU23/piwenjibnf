package com.asset.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 资产申请提交参数
 */
@Data
public class ApplicationDTO {

    @NotNull(message = "请选择申请分类")
    private Long categoryId;

    /** 指定具体资产（可选） */
    private Long assetId;

    /** 申请数量（按分类申请时使用，默认 1） */
    private Integer quantity;

    @NotBlank(message = "请填写申请事由")
    private String reason;
}
