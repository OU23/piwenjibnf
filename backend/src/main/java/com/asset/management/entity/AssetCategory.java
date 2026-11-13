package com.asset.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资产分类（多级树形结构）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_category")
public class AssetCategory extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分类 ID，顶级为 0 */
    private Long parentId;

    /** 分类名称 */
    private String categoryName;

    /** 分类编码（唯一） */
    private String categoryCode;

    /** 层级：1/2/3... */
    private Integer level;

    /** 排序 */
    private Integer sort;

    /** 分类描述 */
    private String description;

    /** 分类属性配置（JSON 数组，定义该分类下资产需维护的扩展字段） */
    private String attributes;

    /** 状态：0 禁用 / 1 启用 */
    private Integer status;
}
