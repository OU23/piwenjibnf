package com.asset.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 资产分类新增/修改参数 & 树节点
 */
@Data
public class CategoryDTO {

    private Long id;

    /** 父分类 ID */
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;

    private Integer sort;

    private String description;

    /** 分类属性配置（JSON 数组字符串） */
    private String attributes;

    private Integer status;

    private List<CategoryDTO> children = new ArrayList<>();
}
