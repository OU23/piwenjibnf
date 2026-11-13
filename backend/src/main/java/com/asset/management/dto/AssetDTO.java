package com.asset.management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产新增/修改参数
 */
@Data
public class AssetDTO {

    private Long id;

    @NotBlank(message = "资产名称不能为空")
    private String assetName;

    /** 资产编号（可自动生成） */
    private String assetCode;

    @NotNull(message = "请选择资产分类")
    private Long categoryId;

    private String brand;

    private String model;

    private String serialNumber;

    private BigDecimal price;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    private String location;

    private String remark;
}
