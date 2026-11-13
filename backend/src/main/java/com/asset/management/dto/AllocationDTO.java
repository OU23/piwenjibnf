package com.asset.management.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 资产分配参数
 */
@Data
public class AllocationDTO {

    /** 待分配的资产 ID 列表 */
    @NotEmpty(message = "请选择要分配的资产")
    private List<Long> assetIds;

    /** 分配目标用户 ID */
    @NotNull(message = "请选择分配对象")
    private Long toUserId;

    /** 分配方式：MANUAL / AUTO */
    private String allocationType;

    /** 备注 */
    private String remark;
}
